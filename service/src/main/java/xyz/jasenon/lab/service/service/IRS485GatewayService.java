package xyz.jasenon.lab.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.jasenon.lab.common.entity.device.gateway.RS485Gateway;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.gateway.CreateRS485Gateway;
import xyz.jasenon.lab.service.dto.gateway.DeleteRS485Gateway;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
public interface IRS485GatewayService extends IService<RS485Gateway> {

    R createRS485Gateway(CreateRS485Gateway createRS485Gateway);

    R deleteRS485Gateway(DeleteRS485Gateway deleteRS485Gateway);

}
