package xyz.jasenon.lab.service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.annotation.RequestPermission;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.gateway.CreateRS485Gateway;
import xyz.jasenon.lab.service.dto.gateway.CreateSocketGateway;
import xyz.jasenon.lab.service.dto.gateway.DeleteRS485Gateway;
import xyz.jasenon.lab.service.dto.gateway.DeleteSocketGateway;
import xyz.jasenon.lab.service.service.IRS485GatewayService;
import xyz.jasenon.lab.service.service.ISocketGatewayService;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@RestController
@RequestMapping("/gateway")
@CrossOrigin(origins = "*", allowCredentials = "true")
public class GatewayController {

    @Autowired
    private IRS485GatewayService rs485GatewayService;
    @Autowired
    private ISocketGatewayService socketGatewayService;


    @RequestPermission(allowed = {Permissions.DEVICE_ADD})
    @PostMapping("/create/rs485")
    public R createRS485Gateway(@RequestBody CreateRS485Gateway createRS485Gateway) {
        return rs485GatewayService.createRS485Gateway(createRS485Gateway);
    }

    @RequestPermission(allowed = {Permissions.DEVICE_ADD})
    @PostMapping("/create/socket")
    public R createSocketGateway(@RequestBody CreateSocketGateway createSocketGateway) {
        return socketGatewayService.createSocketGateway(createSocketGateway);
    }

    @RequestPermission(allowed = {Permissions.DEVICE_ADD})
    @DeleteMapping("/delete/rs485")
    public R deleteRS485Gateway(@RequestBody DeleteRS485Gateway deleteRS485Gateway){
        return rs485GatewayService.deleteRS485Gateway(deleteRS485Gateway);
    }

    @RequestPermission(allowed = {Permissions.DEVICE_ADD})
    @DeleteMapping("/delete/socket")
    public R deleteSocketGateway(@RequestBody DeleteSocketGateway deleteSocketGateway){
        return socketGatewayService.deleteSocketGateway(deleteSocketGateway);
    }

}
