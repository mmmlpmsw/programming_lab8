package ru.n4d9.server;

import ru.n4d9.Utils.Utilities;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Пишет логи
 */
public class Logger {
    private PrintStream out, err;

    private boolean showVerbose = true;
    private boolean showLog = true;
    private boolean showWarn = true;
    private boolean showErr = true;

    Logger(PrintStream logsOut, PrintStream errOut) {
        out = logsOut == null ? getNullStream() : logsOut;
        err = errOut == null ? getNullStream() : errOut;
    }

    /**
     * Если в функцию передать <code>true</code>, будет выводиться подробный отчет
     *
     * @param v режим вывода подробного отчета
     */
    public void setShowVerbose(boolean v) {
        showVerbose = v;
    }

    /**
     * Если в функцию передать <code>true</code>, будет выводиться стандартный отчет
     *
     * @param v режим вывода стандартного отчета
     */
    public void setShowLog(boolean v) {
        showLog = v;
    }

    /**
     * Если в функцию передать <code>true</code>, будут выводиться предупреждения
     *
     * @param v режим вывода предупреждений
     */
    public void setShowWarn(boolean v) {
        showWarn = v;
    }

    /**
     * Если в функцию передать <code>true</code>, будут выводиться ошибки
     * @param v режим вывода ошибок
     */
    public void setShowErr(boolean v) {
        showErr = v;
    }

    /**
     * Для подробного отчета о работе сервера
     *
     * @param message сообщение
     */
    void verbose(String message) {
        if (showVerbose)
            out.println(generateLogTime() + "[ VERBOSE ] " + message);
    }

    /**
     * Для стандартного отчета о работе сервера
     *
     * @param message сообщение
     */
    void log(String message) {
        if (showLog)
            out.println(generateLogTime() + "[ LOG ] " + message);
    }

    /**
     * Для некритичных проблем сервера
     * Предупреждение - сообщение о проблеме, не приводящее к остановке сервера.
     *
     * @param message предупреждение
     */
    void warn(String message) {
        if (showWarn)
            out.println(Utilities.colorize("[[yellow]]" + generateLogTime() + "[ WARNING ] " + message + "[[reset]]"));
    }

    /**
     * Для серьезных проблем сервера, которые могут привести к остановке
     *
     * @param message сообщение проблемы
     */
    void err(String message) {
        if (showErr)
            err.println(Utilities.colorize("[[red]]" + generateLogTime() + "[ ERROR ] " + message + "[[reset]]"));
    }

    /**
     * Возвращает поток, который ничего не делает
     *
     * @return поток-пустышка
     */
    private PrintStream getNullStream() {
        return new PrintStream(new OutputStream() {public void write(int b) {}});
    }

    /**
     * Генерирует строку с временем для сообщений отчета
     *
     * @return строка с временем в читабельном виде
     */
    private String generateLogTime() {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(new Date());
        return String.format("[%02d.%02d.%s %02d:%02d:%02d] ",
                calendar.get(Calendar.DATE),
                calendar.get(Calendar.MONTH)+1,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND)
        );
    }
}
