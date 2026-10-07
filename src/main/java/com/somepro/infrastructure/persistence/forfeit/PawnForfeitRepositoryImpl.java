package com.somepro.infrastructure.persistence.forfeit;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.common.exception.BizException;
import com.somepro.domain.collateral.model.CollateralStatus;
import com.somepro.domain.forfeit.model.ForfeitQuery;
import com.somepro.domain.forfeit.model.ForfeitView;
import com.somepro.domain.forfeit.model.PawnForfeit;
import com.somepro.domain.forfeit.repository.PawnForfeitRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.ticket.model.PawnTicket;
import com.somepro.domain.ticket.model.TicketStatus;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.forfeit.converter.PawnForfeitPoConverter;
import com.somepro.infrastructure.persistence.forfeit.po.PawnForfeitPO;
import com.somepro.infrastructure.persistence.ticket.PawnTicketMapper;
import com.somepro.infrastructure.persistence.ticket.TicketCollateralMapper;
import com.somepro.infrastructure.persistence.ticket.po.PawnTicketPO;
import com.somepro.infrastructure.persistence.ticket.po.TicketCollateralPO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 绝当处置仓储适配器（基础设施层）：MyBatis-Plus 阻塞 JDBC 经 blocking(...) 桥接进响应式链路。
 *
 * 本类三处关键业务语义（与赎当模块同一套手法）：
 *
 * 1. 同票只绝一回（含并发重复递交）
 * *    办理先抢 MySQL 命名锁 GET_LOCK('pawn_forfeit:write')（全实例互斥），锁内事务里先对当票做
 *    条件更新：id 命中、状态仍是 ACTIVE 才翻成 FORFEITED。柜台手快重复递交同一张票，第二笔拿到
 *    锁时票面状态已被第一笔翻走，条件不成立、更新 0 行，整段回滚 —— 处置单只落一条，
 *    票和物也只翻一次。票在办理瞬间被赎回/撤销（状态离开 ACTIVE）同样挡回。
 *    （「逾期满三十天」等单笔规则在 PawnForfeit 聚合里，本类只管跨聚合并发约束。）
 *
 * 2. 处置单号生成 JD-yyyy-NNNN
 *    同一把写锁内：取当年处置单号的最大整数序号 +1（序号在 Java 侧解析，
 *    避免字符串排序把 9999 排在 10000 前），锁内算号天然不撞；
 *    取号刻意包含已删除的处置单：单号一经分配永久占用。
 *    uk_forfeit_no 唯一索引是最后防线，极端瞬态冲突整段重试，不甩底层错给柜台。
 *
 * 3. 票物状态联动与处置单写入同一事务
 *    当票「在当 → 已绝当」、当物「已典当 → 已绝当」与处置单写入在同一事务里落库，
 *    两头状态一起翻，要么一起成、要么一起回滚，不会出现「票已绝、物还押着」的裂账。
 *    当物只翻当前是「已典当」的，别踩了别的流程置的状态。
 *
 * 锁的连接与时序同赎当模块：用一条【独立于事务的原始连接】在事务开启前 GET_LOCK、
 * 在事务【提交之后】才 RELEASE_LOCK，避免「锁已放、事务未提交」导致后到者漏看刚翻走的状态。
 *
 * 翻单（page/findViewById）只读：处置单 JOIN 当票拿票号与票面快照、LEFT JOIN 当物拿编号名称，
 * 欠款本息与盈亏由 ForfeitView.assemble 按赎当同口径现算，不写任何库。
 */
@Repository
public class PawnForfeitRepositoryImpl implements PawnForfeitRepository {

    /** 业务日期统一按行里所在时区算，避免容器 UTC 下单号跨年。 */
    private static final ZoneId BIZ_ZONE = ZoneId.of("Asia/Shanghai");
    /** 绝当办理临界区命名锁（MySQL 全实例同名互斥）。 */
    private static final String WRITE_LOCK = "pawn_forfeit:write";
    private static final int LOCK_WAIT_SECONDS = 10;
    private static final int MAX_RETRY = 5;

    private final PawnForfeitMapper pawnForfeitMapper;
    private final PawnTicketMapper pawnTicketMapper;
    private final TicketCollateralMapper ticketCollateralMapper;
    private final TransactionTemplate transactionTemplate;
    private final DataSource dataSource;

    public PawnForfeitRepositoryImpl(PawnForfeitMapper pawnForfeitMapper,
                                     PawnTicketMapper pawnTicketMapper,
                                     TicketCollateralMapper ticketCollateralMapper,
                                     PlatformTransactionManager transactionManager,
                                     DataSource dataSource) {
        this.pawnForfeitMapper = pawnForfeitMapper;
        this.pawnTicketMapper = pawnTicketMapper;
        this.ticketCollateralMapper = ticketCollateralMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.dataSource = dataSource;
    }

    @Override
    public Mono<PawnForfeit> insert(PawnForfeit forfeit) {
        return blocking(() -> {
            // 每轮重试用独立连接重新抢锁；兜住单号撞号 / 锁等待超时等瞬态冲突
            for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
                try {
                    return inWriteLock(() -> transactionTemplate.execute(status -> {
                        // 同票只绝一回：条件更新「在当 → 已绝当」才作数。
                        // 重复递交的第二笔状态已不在当，更新 0 行，整段回滚不落记录。
                        PawnTicketPO ticketUpdate = new PawnTicketPO();
                        ticketUpdate.setStatus(TicketStatus.FORFEITED.code());
                        int rows = pawnTicketMapper.update(ticketUpdate,
                                Wrappers.<PawnTicketPO>lambdaUpdate()
                                        .eq(PawnTicketPO::getId, forfeit.getTicketId())
                                        .eq(PawnTicketPO::getStatus, TicketStatus.ACTIVE.code()));
                        if (rows == 0) {
                            throw new BizException("当票状态已变化，本次绝当未生效；请刷新后按最新票面办理");
                        }
                        // 票物联动：票绝了，押的当物跟着从已典当转已绝当，同一事务一起翻。
                        // 当物 id 以库里的票面为准（同事务内读，刚翻过的状态本连接可见）。
                        PawnTicketPO ticket = pawnTicketMapper.selectById(forfeit.getTicketId());
                        if (ticket == null) {
                            throw new BizException("当票不存在");
                        }
                        markCollateralForfeited(ticket.getCollateralId());
                        PawnForfeitPO po = PawnForfeitPoConverter.toPo(forfeit);
                        po.setId(IdUtil.getSnowflakeNextId());
                        po.setForfeitNo(nextForfeitNo());
                        // 当物 id 以库里票面为准，与上面联动的是同一件东西
                        po.setCollateralId(ticket.getCollateralId());
                        pawnForfeitMapper.insert(po);
                        return PawnForfeitPoConverter.toDomain(po);
                    }));
                } catch (DuplicateKeyException | TransientDataAccessException e) {
                    // uk_forfeit_no 是最后防线，锁内正常不会撞；撞了整段重新取号重试
                    if (attempt == MAX_RETRY - 1) {
                        throw new BizException("系统繁忙，请稍后重试");
                    }
                    try {
                        Thread.sleep(10L * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BizException("系统繁忙，请稍后重试");
                    }
                }
            }
            throw new BizException("系统繁忙，请稍后重试");
        });
    }

    @Override
    public Mono<PawnForfeit> findById(Long id) {
        return blocking(() -> {
            PawnForfeitPO po = pawnForfeitMapper.selectById(id);
            return po == null ? null : PawnForfeitPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<PawnForfeit> findByForfeitNo(String forfeitNo) {
        return blocking(() -> {
            PawnForfeitPO po = pawnForfeitMapper.selectOne(
                    Wrappers.<PawnForfeitPO>lambdaQuery().eq(PawnForfeitPO::getForfeitNo, forfeitNo));
            return po == null ? null : PawnForfeitPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<ForfeitView> findViewById(Long id) {
        return blocking(() -> toView(pawnForfeitMapper.selectViewById(id)));
    }

    @Override
    public Mono<ForfeitView> findViewByForfeitNo(String forfeitNo) {
        return blocking(() -> toView(pawnForfeitMapper.selectViewByForfeitNo(forfeitNo)));
    }

    @Override
    public Mono<PageResult<ForfeitView>> page(int pageNum, int pageSize, ForfeitQuery query) {
        Long ticketId = query == null ? null : query.ticketId();
        List<String> methods = query == null || query.disposeMethod() == null
                ? Collections.emptyList()
                : Collections.singletonList(query.disposeMethod().code());
        return this.<PageResult<ForfeitView>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                List<ForfeitListRow> rows = pawnForfeitMapper.selectViewPage(ticketId, methods);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<ForfeitView> content = rows.stream()
                        .map(PawnForfeitRepositoryImpl::toView)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                // PageHelper 靠 ThreadLocal 传分页参数，必须清，避免污染线程池下一次调用
                PageHelper.clearPage();
            }
        });
    }

    /**
     * 投影行 → 对账视图：重建处置单领域对象与票面快照（只填欠款试算用得到的列），
     * 交 ForfeitView.assemble 按处置时刻走赎当同口径算欠款本息与盈亏差额。
     */
    private static ForfeitView toView(ForfeitListRow row) {
        if (row == null) {
            return null;
        }
        PawnForfeit forfeit = new PawnForfeit();
        forfeit.setId(row.getId());
        forfeit.setForfeitNo(row.getForfeitNo());
        forfeit.setTicketId(row.getTicketId());
        forfeit.setCollateralId(row.getCollateralId());
        forfeit.setForfeitedAt(row.getForfeitedAt());
        forfeit.setRecoverAmount(row.getRecoverAmount());
        forfeit.setDisposeMethod(row.getDisposeMethod() == null
                ? null : com.somepro.domain.forfeit.model.DisposeMethod.valueOf(row.getDisposeMethod()));
        forfeit.setCreateTime(row.getCreateTime());

        PawnTicket ticket = new PawnTicket();
        ticket.setId(row.getTicketId());
        ticket.setTicketNo(row.getTicketNo());
        ticket.setCollateralId(row.getCollateralId());
        ticket.setPawnAmount(row.getPawnAmount());
        ticket.setMonthlyRate(row.getMonthlyRate());
        ticket.setServiceRate(row.getServiceRate());
        ticket.setStartDate(row.getStartDate());
        // 欠款试算只认上面这些快照列；票此刻已是 FORFEITED，但 settle 不卡票状态，无需伪造状态。

        return ForfeitView.assemble(forfeit, ticket, row.getItemNo(), row.getItemName());
    }

    /**
     * 生成 JD-年份-序号：序号是当年已有处置单号（含已删除）最大整数 +1，至少 4 位、超出自然进位。
     * 只在写锁（{@link #inWriteLock}）内调用，锁内串行所以不会撞号；
     * forfeit_no 唯一索引是最后防线，极端瞬态冲突由外层整段重试兜底。
     */
    private String nextForfeitNo() {
        int year = LocalDate.now(BIZ_ZONE).getYear();
        String prefix = "JD-" + year + "-";
        long maxSeq = 0L;
        for (String no : pawnForfeitMapper.findForfeitNosByPrefix(prefix + "%")) {
            if (no == null || !no.startsWith(prefix)) {
                continue;
            }
            String tail = no.substring(prefix.length());
            if (tail.chars().allMatch(Character::isDigit)) {
                maxSeq = Math.max(maxSeq, Long.parseLong(tail));
            }
        }
        return prefix + String.format("%04d", maxSeq + 1);
    }

    /**
     * 当物状态联动：已典当 → 已绝当。只翻当前状态是「已典当」的行，别踩了别的流程置的状态。
     * 更新 0 行即状态已被人动过，抛业务异常让整段事务回滚，票、物、处置单几处都不落。
     */
    private void markCollateralForfeited(Long collateralId) {
        TicketCollateralPO update = new TicketCollateralPO();
        update.setStatus(CollateralStatus.FORFEITED.code());
        int rows = ticketCollateralMapper.update(update,
                Wrappers.<TicketCollateralPO>lambdaUpdate()
                        .eq(TicketCollateralPO::getId, collateralId)
                        .eq(TicketCollateralPO::getStatus, CollateralStatus.PAWNED.code()));
        if (rows == 0) {
            throw new BizException("当物状态已变化，本次绝当未生效；请刷新后按最新状态办理");
        }
    }

    /**
     * 在全局命名锁保护下执行一段【含事务】的写入：锁由一条独立原始连接持有，
     * 在事务开始前 GET_LOCK、在事务提交/回滚之后才 RELEASE_LOCK（顺序不能颠倒）。
     *
     * 为什么锁要走独立连接而不是 MyBatis 连接：GET_LOCK 绑定连接；
     * 若用事务所在连接，Spring 提交时归还连接会立刻放锁，存在「锁已放、事务未提交」的窗口，
     * 后到的事务取号/点状态时读不到刚提交的数据，会算出重复号、漏看刚翻走的状态。
     * 独立连接持锁可把锁保到提交之后。
     */
    private <T> T inWriteLock(Supplier<T> action) {
        Connection lockConn;
        try {
            lockConn = dataSource.getConnection();
        } catch (SQLException e) {
            throw new BizException("系统繁忙，请稍后重试");
        }
        try {
            if (!namedLock(lockConn, true)) {
                throw new BizException("系统繁忙，请稍后重试");
            }
            try {
                return action.get();
            } finally {
                // 此时 action 内的事务已提交（或回滚），放锁后后到者必能看到本次写入
                namedLock(lockConn, false);
            }
        } finally {
            try {
                lockConn.close();
            } catch (SQLException ignored) {
                // 连接关闭会自动释放其上的命名锁，不影响主流程
            }
        }
    }

    /** GET_LOCK / RELEASE_LOCK；返回 MySQL 结果（1 成功）。 */
    private boolean namedLock(Connection conn, boolean get) {
        String sql = get ? "SELECT GET_LOCK(?, ?)" : "SELECT RELEASE_LOCK(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, WRITE_LOCK);
            if (get) {
                ps.setInt(2, LOCK_WAIT_SECONDS);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int r = rs.getInt(1);
                    return !rs.wasNull() && r == 1;
                }
                return false;
            }
        } catch (SQLException e) {
            if (get) {
                throw new BizException("系统繁忙，请稍后重试");
            }
            return false;
        }
    }

    /**
     * 阻塞 DB 调用 → 响应式链路桥接器：先从 Reactor Context 取操作人，再切到 boundedElastic，
     * 操作人放进 AuditContextHolder 供审计填充（与赎当/续当模块同一套约定，顺序不能颠倒）。
     */
    private <T> Mono<T> blocking(Supplier<T> supplier) {
        return Mono.deferContextual(ctx -> {
            String operator = ReactiveOperatorContext.getOperator(ctx);
            return Mono.fromCallable(() -> {
                AuditContextHolder.setOperator(operator);
                try {
                    return supplier.get();
                } finally {
                    AuditContextHolder.clear();
                }
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }
}
