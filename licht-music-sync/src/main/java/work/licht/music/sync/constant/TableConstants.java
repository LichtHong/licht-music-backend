package work.licht.music.sync.constant;

// 表常量类
public class TableConstants {

    // 表名中的分隔符
    private static final String TABLE_NAME_SEPARATE = "_";

    // 拼接表名后缀
    public static String buildTableNameSuffix(String date, long hashKey) {
        // 拼接完整的表名
        return date + TABLE_NAME_SEPARATE + hashKey;
    }

}