package org.unicode.cldr.unittest;

import java.util.concurrent.Callable;

import com.ibm.icu.text.MessageFormat;

/**
 * Class for holding error reports
 *
 * @author ribnitz
 *
 */
public class CheckResult {
    /**
     * The status of a CheckResult
     *
     * @author ribnitz
     *
     */
    public enum ResultStatus {
        error, warning;
    }

    CheckResult.ResultStatus status;
    String message;
    String locale;
    String path;

    public String getLocale() {
        return locale;
    }

    public String getPath() {
        return path;
    }

    public CheckResult setPath(String path) {
        this.path = path;
        return this;
    }

    public CheckResult setLocale(String locale) {
        this.locale = locale;
        return this;
    }

    public CheckResult() {
    }

    public CheckResult setMessage(String msg, Object[] args) {
        message = MessageFormat.format(msg, args);
        return this;
    }

    public CheckResult.ResultStatus getStatus() {
        return status;
    }

    public CheckResult setStatus(CheckResult.ResultStatus status) {
        this.status = status;
        return this;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Factory method, initialize with (status,locale,path); depending on the
     * result of pred, use ether (msgSuccess,objSuccess) or (msgFail,objFail) to
     * construct the message.
     *
     * @param status
     * @param locale
     * @param path
     * @param pred
     * @param msgSuccess
     * @param msgFail
     * @param objSuccess
     * @param objFail
     * @return newly constructed CheckResult or null, in the case of an error
     *         occurring on Callable invocation
     */
    public static CheckResult create(CheckResult.ResultStatus status,
        String locale, String path, Callable<Boolean> pred,
        String msgSuccess, String msgFail, Object[] objSuccess,
        Object[] objFail) {
        if (pred == null) {
            throw new IllegalArgumentException("The callable must not be null");
        }
        try {
            CheckResult result = new CheckResult().setStatus(status)
                .setLocale(locale).setPath(path);
            if (pred.call()) {
                result.setMessage(msgSuccess, objSuccess);
            } else {
                result.setMessage(msgFail, objFail);
            }
            return result;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}