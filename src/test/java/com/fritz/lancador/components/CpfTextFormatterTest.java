package com.fritz.lancador.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CpfTextFormatterTest {
    @Test
    void formatsCpfAsDigitsAreEntered() {
        assertEquals("529", CpfTextFormatter.format("529"));
        assertEquals("529.982", CpfTextFormatter.format("529982"));
        assertEquals("529.982.247-25", CpfTextFormatter.format("52998224725"));
    }

    @Test
    void formatsPastedCpfAndIgnoresCharactersAfterElevenDigits() {
        assertEquals("529.982.247-25", CpfTextFormatter.format("529.982.247-25abc9"));
    }
}
