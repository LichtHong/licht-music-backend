package work.licht.music.id.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import work.licht.music.id.constant.Constants;
import work.licht.music.id.core.IDGen;
import work.licht.music.id.core.common.PropertyFactory;
import work.licht.music.id.core.common.Result;
import work.licht.music.id.core.common.ZeroIDGen;
import work.licht.music.id.core.snowflake.SnowflakeIDGenImpl;
import work.licht.music.id.exception.InitException;

import java.util.Properties;

@Service("SnowflakeService")
public class SnowflakeService {

    private final IDGen idGen;

    public SnowflakeService() throws InitException {
        Properties properties = PropertyFactory.getProperties();
        boolean flag = Boolean.parseBoolean(properties.getProperty(Constants.LEAF_SNOWFLAKE_ENABLE, "true"));
        Logger logger = LoggerFactory.getLogger(SnowflakeService.class);
        if (flag) {
            String zkAddress = properties.getProperty(Constants.LEAF_SNOWFLAKE_ZK_ADDRESS);
            int port = Integer.parseInt(properties.getProperty(Constants.LEAF_SNOWFLAKE_PORT));
            idGen = new SnowflakeIDGenImpl(zkAddress, port);
            if (idGen.init()) {
                logger.info("Snowflake Service Init Successfully");
            } else {
                throw new InitException("Snowflake Service Init Fail");
            }
        } else {
            idGen = new ZeroIDGen();
            logger.info("Zero ID Gen Service Init Successfully");
        }
    }

    public Result getId(String key) {
        return idGen.get(key);
    }
}
