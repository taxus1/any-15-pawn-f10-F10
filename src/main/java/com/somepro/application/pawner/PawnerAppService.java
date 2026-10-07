package com.somepro.application.pawner;

import com.somepro.common.exception.BizException;
import com.somepro.domain.pawner.model.Pawner;
import com.somepro.domain.pawner.model.PawnerLedger;
import com.somepro.domain.pawner.model.PawnerQuery;
import com.somepro.domain.pawner.model.PawnerStatus;
import com.somepro.domain.pawner.repository.PawnerLedgerPort;
import com.somepro.domain.pawner.repository.PawnerRepository;
import com.somepro.domain.shared.model.PageResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 当户应用服务：编排录入、修改、详情、注销、冻结、解冻、翻名单等用例，不写表映射。
 *
 * 出入参用领域对象/基础类型，不认识 PO 与 VO。
 * 唯一性、编号生成等落库规则在仓储里；档案自身规则在 Pawner 聚合里；
 * 注销前「名下是否干净」在这里编排：先查对账端口，再调聚合的注销行为。
 */
@Service
public class PawnerAppService {

    private final PawnerRepository pawnerRepository;
    private final PawnerLedgerPort ledgerPort;

    public PawnerAppService(PawnerRepository pawnerRepository, PawnerLedgerPort ledgerPort) {
        this.pawnerRepository = pawnerRepository;
        this.ledgerPort = ledgerPort;
    }

    /** 录入：默认 NORMAL；同身份证已有未注销档案由仓储挡回，已注销的历史档案不影响重新建档。 */
    public Mono<Pawner> register(String name, String idCard, String phone, String address) {
        Pawner pawner = Pawner.register(name, idCard, phone, address);
        return pawnerRepository.insert(pawner);
    }

    /** 修改档案：只改姓名/身份证/电话/地址；状态不在这里夹带，冻结解冻走专门用例、注销走 /close。 */
    public Mono<Pawner> update(Long id, String name, String idCard, String phone, String address) {
        return requirePawner(id).flatMap(pawner -> {
            pawner.modify(name, idCard, phone, address);
            return pawnerRepository.update(pawner);
        });
    }

    /**
     * 冻结：把正常当户冻成 FROZEN，办理时刻与经办人随审计列落账。
     * 连着点多回也只算头一回（幂等，不覆盖头一次的办理记录）；已注销的冻不了，仓储会说明情况。
     * 判定与落库是仓储里同一条行级条件更新，原子完成，不在应用层先读再写（避免并发下拿旧状态覆盖）。
     */
    public Mono<Pawner> freeze(Long id) {
        if (id == null) {
            return Mono.error(new BizException("必须指定要冻结的当户"));
        }
        return pawnerRepository.freeze(id);
    }

    /**
     * 解冻：把冻结当户放回 NORMAL。连着点多回只算头一回（幂等）；
     * 注销（CLOSED）的不能解，仓储会单独说明情况。并发语义同 {@link #freeze}。
     */
    public Mono<Pawner> unfreeze(Long id) {
        if (id == null) {
            return Mono.error(new BizException("必须指定要解冻的当户"));
        }
        return pawnerRepository.unfreeze(id);
    }

    /** 详情：带出档案 + 名下对账三个数（在押/在库/在当当票），数与当物、当票两表实时一致。 */
    public Mono<PawnerDetail> detail(Long id, String pawnerNo) {
        Mono<Pawner> found;
        if (id != null) {
            found = requirePawner(id);
        } else if (pawnerNo != null && !pawnerNo.isBlank()) {
            found = pawnerRepository.findByPawnerNo(pawnerNo.trim())
                    .switchIfEmpty(Mono.error(new BizException("当户不存在")));
        } else {
            return Mono.error(new BizException("请指定要查看的当户（id 或 pawnerNo）"));
        }
        return found.flatMap(pawner -> ledgerPort.load(pawner.getId())
                .map(ledger -> new PawnerDetail(pawner, ledger)));
    }

    /**
     * 注销：名下还压着没走完的当物（IN_STOCK/PAWNED），或还挂着在当当票（ACTIVE），一律挡回；
     * 干干净净才销得动。销完状态置 CLOSED（账留着），默认名单不再出现。
     */
    public Mono<Pawner> close(Long id) {
        return requirePawner(id).flatMap(pawner -> ledgerPort.load(pawner.getId()).flatMap(ledger -> {
            if (!ledger.clean()) {
                return Mono.error(new BizException(String.format(
                        "该当户名下尚有未了结业务（在押当物 %d 件、在当当票 %d 笔），不能注销",
                        ledger.heldItemCount(), ledger.activeTicketCount())));
            }
            pawner.close();
            return pawnerRepository.update(pawner);
        }));
    }

    /** 翻名单：姓名/身份证/电话/状态随意拼，都不填翻整份；默认不含已注销。 */
    public Mono<PageResult<Pawner>> page(int pageNum, int pageSize,
                                         String name, String idCard, String phone, String status) {
        if (pageNum < 1 || pageSize < 1) {
            return Mono.error(new BizException("页码与每页条数必须为正整数"));
        }
        PawnerQuery query = PawnerQuery.of(name, idCard, phone, PawnerStatus.ofCode(status));
        return pawnerRepository.page(pageNum, pageSize, query);
    }

    private Mono<Pawner> requirePawner(Long id) {
        return pawnerRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("当户不存在")));
    }

    /** 应用层内部组合值：档案 + 对账快照，接口层据此转详情 VO。 */
    public record PawnerDetail(Pawner pawner, PawnerLedger ledger) {
    }
}
