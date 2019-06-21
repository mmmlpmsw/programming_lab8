package ru.n4d9.server;

import ru.n4d9.Utils.Utilities;

import java.io.PrintStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Пишет логи
 */
class Logger {
    private PrintStream out1, out2;
    private PrintStream err1, err2;

    Logger(PrintStream logsOutput1, PrintStream logsOutput2, PrintStream errorsOutput1, PrintStream errorsOutput2) {
        this.out1 = logsOutput1;
        this.out2 = logsOutput2;
        this.err1 = errorsOutput1;
        this.err2 = errorsOutput2;
    }

    void log(String message) {
        out1.println(generateLogTime() + message);
        out1.flush();
        out2.println(generateLogTime() + message);
        out2.flush();
    }

    void err(String message) {
        err1.println(Utilities.colorize("[[red]]" + generateLogTime() + "[ ERROR ] " + message + "[[reset]]"));
        err1.flush();
        err2.println(Utilities.colorize("[[red]]" + generateLogTime() + "[ ERROR ] " + message + "[[reset]]"));
        err2.flush();
    }

    void warn(String message) {
        err1.println(Utilities.colorize("[[yellow]]" + generateLogTime() + "[ WARNING ] " + message + "[[reset]]"));
        err1.flush();
        err2.println(Utilities.colorize("[[yellow]]" + generateLogTime() + "[ WARNING ] " + message + "[[reset]]"));
        err2.flush();
    }

    void longErr(String message) {

    }

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
