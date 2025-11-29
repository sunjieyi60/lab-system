package xyz.jasenon.lab.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import xyz.jasenon.lab.service.constants.Permissions;

@Slf4j
class ServiceApplicationTests {

	@Test
	@SneakyThrows
	void contextLoads() {
		ObjectMapper objectMapper = new ObjectMapper();
		log.info("permission tree:{}", objectMapper.writeValueAsString(Permissions.treeAll()));
		log.info("USER_ADD_PATH:{}",Permissions.pathOf(Permissions.DEVICE_CONTROL));
	}

}
