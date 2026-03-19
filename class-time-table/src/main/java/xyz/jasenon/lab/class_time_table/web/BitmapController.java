package xyz.jasenon.lab.class_time_table.web;

import cn.hutool.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xyz.jasenon.lab.class_time_table.service.BitmapService;
import xyz.jasenon.lab.class_time_table.vo.BitmapPushResultVo;
import xyz.jasenon.lab.common.utils.R;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/bitmap")
@CrossOrigin("*")
@RequiredArgsConstructor
public class BitmapController {

    private final BitmapService bitmapService;

    @PostMapping("/send")
    public R<BitmapPushResultVo> addFaceFutureToClassTimeTable(@RequestParam MultipartFile file,
                                                                @RequestParam String faceName,
                                                                @RequestParam(required = false) List<String> uuids,
                                                                @RequestParam(required = false) List<String> groupIds) {
        if (Objects.isNull(uuids) && Objects.isNull(groupIds)) {
            return R.fail(HttpStatus.HTTP_BAD_REQUEST, "必须传递组号或者设备编号列表");
        }

        return bitmapService.pushBitmapToClassTimeTable(file, faceName, uuids, groupIds);
    }
}
