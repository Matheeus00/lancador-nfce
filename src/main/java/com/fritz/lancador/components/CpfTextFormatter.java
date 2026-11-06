package com.fritz.lancador.components;

import javafx.scene.control.TextFormatter;

import java.util.function.UnaryOperator;

public final class CpfTextFormatter {
    private CpfTextFormatter() {
    }

    public static TextFormatter<String> create() {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            if (!change.isContentChange()) {
                return change;
            }

            int anchorDigits = countDigits(change.getControlNewText(), change.getAnchor());
            int caretDigits = countDigits(change.getControlNewText(), change.getCaretPosition());
            String formatted = format(change.getControlNewText());

            change.setRange(0, change.getControlText().length());
            change.setText(formatted);
            change.setAnchor(positionAfterDigits(formatted, anchorDigits));
            change.setCaretPosition(positionAfterDigits(formatted, caretDigits));
            return change;
        };
        return new TextFormatter<>(filter);
    }

    static String format(String input) {
        String digits = input.replaceAll("\\D", "");
        if (digits.length() > 11) {
            digits = digits.substring(0, 11);
        }

        StringBuilder formatted = new StringBuilder(digits.length() + 3);
        for (int i = 0; i < digits.length(); i++) {
            if (i == 3 || i == 6) {
                formatted.append('.');
            } else if (i == 9) {
                formatted.append('-');
            }
            formatted.append(digits.charAt(i));
        }
        return formatted.toString();
    }

    private static int countDigits(String value, int end) {
        int count = 0;
        for (int i = 0; i < Math.min(end, value.length()); i++) {
            if (Character.isDigit(value.charAt(i))) {
                count++;
            }
        }
        return Math.min(count, 11);
    }

    private static int positionAfterDigits(String formatted, int digitCount) {
        if (digitCount <= 0) {
            return 0;
        }

        int digitsSeen = 0;
        for (int i = 0; i < formatted.length(); i++) {
            if (Character.isDigit(formatted.charAt(i)) && ++digitsSeen == digitCount) {
                return i + 1;
            }
        }
        return formatted.length();
    }
}
