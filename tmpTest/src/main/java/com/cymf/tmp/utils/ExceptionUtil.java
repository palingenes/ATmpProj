package com.cymf.tmp.utils;


public class ExceptionUtil {

    /**
     * 生成Exception的str
     * @param ex
     * @return
     * @throws NullPointerException
     */
    public static String toString(Exception ex) throws NullPointerException {
        StringBuffer LogText = new StringBuffer();
        LogText.append(ex.toString()). append("\n");
        for(int i=0;i<ex.getStackTrace().length;i++)
            LogText.append("        at "). append(ex.getStackTrace()[i])
                    . append("\n");
        return LogText.toString();
    }

}
