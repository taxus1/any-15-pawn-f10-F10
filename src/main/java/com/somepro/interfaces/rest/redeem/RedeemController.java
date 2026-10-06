package com.somepro.interfaces.rest.redeem;

import com.somepro.application.redeem.PawnRedeemAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.redeem.converter.PawnRedeemVoConverter;
import com.somepro.interfaces.rest.redeem.dto.RedeemCreateRequest;
import com.somepro.interfaces.rest.redeem.vo.PawnRedeemVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 赎当模块用户接口层：办理赎当、查看赎当单、按当票翻赎当记录。
 *
 * 只做协议适配（参数解析、VO 转换、Result 包装），业务编排在 {@link PawnRedeemAppService}。
 * 入参统一走 @ModelAttribute / @RequestParam：表单 / query string / x-www-form-urlencoded 都能接，
 * 便于柜台端直接调用。
 */
@RestController
@RequestMapping("/api/redeem")
public class RedeemController {

    private final PawnRedeemAppService pawnRedeemAppService;

    public RedeemController(PawnRedeemAppService pawnRedeemAppService) {
        this.pawnRedeemAppService = pawnRedeemAppService;
    }

    /**
     * 办理赎当：当场按票面快照算清计费天数、利息与综合费、应还总额，收讫放货。
     * 只有在当的票赎得了，晚于到期日期来赎照收、按实际天数计费不加罚；
     * 同一时点重复递交只成一次；赎当单号服务端按 SD-年份-序号 生成；
     * 办成后当票转已赎、当物转已赎回，两处状态一起翻。
     */
    @PostMapping("/create")
    public Mono<Result<PawnRedeemVO>> create(@ModelAttribute RedeemCreateRequest request) {
        return pawnRedeemAppService.redeem(request.getTicketId())
                .map(PawnRedeemVoConverter::toVo)
                .map(Result::ok);
    }

    /** 查看赎当单：id 或 redeemNo 任一指定。 */
    @GetMapping("/detail")
    public Mono<Result<PawnRedeemVO>> detail(@RequestParam(required = false) Long id,
                                             @RequestParam(required = false) String redeemNo) {
        return pawnRedeemAppService.detail(id, redeemNo)
                .map(PawnRedeemVoConverter::toVo)
                .map(Result::ok);
    }

    /**
     * 按当票翻赎当记录：必传 ticketId，一页一页走。
     * pageNum/pageSize 由请求说了算，每行带 redeemNo 便于与赎当凭证对号。
     */
    @GetMapping("/list")
    public Mono<Result<PageVO<PawnRedeemVO>>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                   @RequestParam(defaultValue = "20") int pageSize,
                                                   @RequestParam Long ticketId) {
        return pawnRedeemAppService.pageByTicket(pageNum, pageSize, ticketId)
                .map(PawnRedeemVoConverter::toPageVo)
                .map(Result::ok);
    }
}
