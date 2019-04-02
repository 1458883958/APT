package com.wdl.apt_processor;

import java.util.Locale;

/**
 * 创建时间： 2019/3/20 15:14
 * 描述：    TODO
 */
@SuppressWarnings("unused")
public class Utils {
    public static String toLowerCaseFirstChar(String str) {
        String first = str.charAt(0) + "";
        return first.toLowerCase() + str.substring(1);
    }
}
