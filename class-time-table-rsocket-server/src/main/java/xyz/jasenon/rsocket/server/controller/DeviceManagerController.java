package xyz.jasenon.rsocket.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.rsocket.core.model.ClassTimeTable;
import xyz.jasenon.rsocket.core.rsocket.AbstractConnectionManager;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/manager")
public class DeviceManagerController {

    private final AbstractConnectionManager connections;

    @GetMapping("/list")
    public R<List<ClassTimeTable>> listAll(@RequestParam(value = "status", required = false) String status){
        return null;
    }

}
