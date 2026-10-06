package com.somepro.interfaces.rest.renew;

import com.somepro.application.renew.PawnRenewAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.renew.converter.PawnRenewVoConverter;
import com.somepro.interfaces.rest.renew.dto.RenewCreateRequest;
import com.somepro.interfaces.rest.renew.vo.PawnRenewVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 续当模块用户接口层：办理续当、查看续当单、按当票翻续当记录。
 *
 * 只做协议适配（参数解析、VO 转换、Result 包装），业务编排在 {@link PawnRenewAppService}。
 * 入参统一走 @ModelAttribute / @RequestParam：表单 / query string / x-www-form-urlencoded 都能接，
 * 便于柜台端直接调用。
 */
@RestController
@RequestMapping("/api/renew")
public class RenewController {

    private final PawnRenewAppService pawnRenewAppService;

    public RenewController(PawnRenewAppService pawnRenewAppService) {
        this.pawnRenewAppService = pawnRenewAppService;
    }

    /**
     * 办理续当：顺延月数按票面原当期走，新到期日期从原到期日期往后推，票仍留在当。
     * 只有在当、且在到期日当天或之前办得了；同一时点重复递交只成一次；续当单号服务端按 XD-年份-序号 生成。
     */
    @PostMapping("/create")
    public Mono<Result<PawnRenewVO>> create(@ModelAttribute RenewCreateRequest request) {
        return pawnRenewAppService.renew(request.getTicketId())
                .map(PawnRenewVoConverter::toVo)
                .map(Result::ok);
    }

    /** 查看续当单：id 或 renewNo 任一指定。 */
    @GetMapping("/detail")
    public Mono<Result<PawnRenewVO>> detail(@RequestParam(required = false) Long id,
                                            @RequestParam(required = false) String renewNo) {
        return pawnRenewAppService.detail(id, renewNo)
                .map(PawnRenewVoConverter::toVo)
                .map(Result::ok);
    }

    /**
     * 按当票翻续当记录：必传 ticketId，一页一页走。
     * pageNum/pageSize 由请求说了算，每行带 renewNo 便于与续当凭证对号。
     */
    @GetMapping("/list")
    public Mono<Result<PageVO<PawnRenewVO>>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                  @RequestParam(defaultValue = "20") int pageSize,
                                                  @RequestParam Long ticketId) {
        return pawnRenewAppService.pageByTicket(pageNum, pageSize, ticketId)
                .map(PawnRenewVoConverter::toPageVo)
                .map(Result::ok);
    }
}
