package com.somepro.interfaces.rest.ticket;

import com.somepro.application.ticket.PawnTicketAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.ticket.converter.PawnTicketVoConverter;
import com.somepro.interfaces.rest.ticket.dto.TicketCreateRequest;
import com.somepro.interfaces.rest.ticket.dto.TicketIdRequest;
import com.somepro.interfaces.rest.ticket.dto.TicketUpdateRequest;
import com.somepro.interfaces.rest.ticket.vo.PawnTicketVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 当票模块用户接口层：开票、修改、详情、撤销、按条件翻票。
 *
 * 只做协议适配（参数解析、VO 转换、Result 包装），业务编排在 {@link PawnTicketAppService}。
 * 入参统一走 @ModelAttribute / @RequestParam：表单 / query string / x-www-form-urlencoded 都能接，
 * 便于柜台端直接调用。
 */
@RestController
@RequestMapping("/api/ticket")
public class TicketController {

    private final PawnTicketAppService pawnTicketAppService;

    public TicketController(PawnTicketAppService pawnTicketAppService) {
        this.pawnTicketAppService = pawnTicketAppService;
    }

    /**
     * 开票：当户/类别/估值随当物带出，利率费率照该类别当前配置抄快照，
     * 当金受折当率上限约束，票号由服务端按 DP-年份-序号 生成，新票落在当。
     */
    @PostMapping("/create")
    public Mono<Result<PawnTicketVO>> create(@ModelAttribute TicketCreateRequest request) {
        return pawnTicketAppService.issue(request.getCollateralId(), request.getPawnAmount(),
                        request.getStartDate(), request.getTermMonths())
                .map(PawnTicketVoConverter::toVo)
                .map(Result::ok);
    }

    /** 修改：当金/起当日期/当期月数，留空不动；只有在当的票改得动，改当金仍受折当率上限约束。 */
    @PostMapping("/update")
    public Mono<Result<PawnTicketVO>> update(@ModelAttribute TicketUpdateRequest request) {
        return pawnTicketAppService.update(request.getId(), request.getPawnAmount(),
                        request.getStartDate(), request.getTermMonths())
                .map(PawnTicketVoConverter::toVo)
                .map(Result::ok);
    }

    /** 撤销：开错的票作废，只有在当的票撤得掉；票置已撤销、当物回在库，时刻由服务端落账。 */
    @PostMapping("/cancel")
    public Mono<Result<PawnTicketVO>> cancel(@ModelAttribute TicketIdRequest request) {
        return pawnTicketAppService.cancel(request.getId())
                .map(PawnTicketVoConverter::toVo)
                .map(Result::ok);
    }

    /** 详情：id 或 ticketNo 任一指定。 */
    @GetMapping("/detail")
    public Mono<Result<PawnTicketVO>> detail(@RequestParam(required = false) Long id,
                                             @RequestParam(required = false) String ticketNo) {
        return pawnTicketAppService.detail(id, ticketNo)
                .map(PawnTicketVoConverter::toVo)
                .map(Result::ok);
    }

    /**
     * 翻票：当户/类别/状态随意拼，都不填翻整份。
     * pageNum/pageSize 由请求说了算，每行带 ticketNo 便于与纸质票根对号。
     */
    @GetMapping("/list")
    public Mono<Result<PageVO<PawnTicketVO>>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                   @RequestParam(defaultValue = "20") int pageSize,
                                                   @RequestParam(required = false) Long pawnerId,
                                                   @RequestParam(required = false) String category,
                                                   @RequestParam(required = false) String status) {
        return pawnTicketAppService.page(pageNum, pageSize, pawnerId, category, status)
                .map(PawnTicketVoConverter::toPageVo)
                .map(Result::ok);
    }
}
