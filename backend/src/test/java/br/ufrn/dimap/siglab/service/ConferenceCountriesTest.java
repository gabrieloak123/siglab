package br.ufrn.dimap.siglab.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConferenceCountriesTest {

    @Test void matchesCountryInTheLastLocationSegment() {
        assertTrue(ConferenceCountries.matches("Krakow, Poland", "Poland"));
        assertTrue(ConferenceCountries.matches("Bari, Italy", "Italy"));
        assertTrue(ConferenceCountries.matches("Montreal, Quebec, Canada", "Canada"));
        assertTrue(ConferenceCountries.matches("São Paulo, Brazil", "Brazil"));
        assertTrue(ConferenceCountries.matches("Amsterdam, the Netherlands", "Netherlands"));
    }

    @Test void acceptsAliasesFromTheApiData() {
        assertTrue(ConferenceCountries.matches("Busan, South Korea", "South Korea"));
        assertTrue(ConferenceCountries.matches("Seoul, Korea", "South Korea"));
        assertTrue(ConferenceCountries.matches("Phoenix, USA", "United States"));
        assertTrue(ConferenceCountries.matches("London, UK", "United Kingdom"));
        assertTrue(ConferenceCountries.matches("Colombo, Srilanka", "Sri Lanka"));
        assertTrue(ConferenceCountries.matches("Dubai, UAE", "United Arab Emirates"));
    }

    @Test void doesNotMatchOtherCountriesOrWords() {
        assertFalse(ConferenceCountries.matches("Beijing, China", "United States"));
        assertFalse(ConferenceCountries.matches("Kyiv, Ukraine", "UK"));
        assertTrue(ConferenceCountries.matches("London, UK", "UK"));
        assertFalse(ConferenceCountries.matches("Online", "Brazil"));
        assertFalse(ConferenceCountries.matches("to be updated", "Poland"));
        assertFalse(ConferenceCountries.matches(null, "Poland"));
    }

    @Test void knowsTheOfferedCountriesIncludingNonTop20() {
        assertTrue(ConferenceCountries.isKnown("Brazil"));
        assertTrue(ConferenceCountries.isKnown("Netherlands"));
        assertTrue(ConferenceCountries.isKnown("United States"));
        assertTrue(ConferenceCountries.isKnown("usa"));
        assertFalse(ConferenceCountries.isKnown("Atlantis"));
        assertFalse(ConferenceCountries.isKnown(" "));
    }

    @Test void listsCountriesAlphabeticallyWithBrazil() {
        var names = ConferenceCountries.names();

        assertTrue(names.contains("Brazil"));
        assertTrue(names.contains("Netherlands"));
        assertTrue(names.contains("Poland"));
        assertEquals(names.stream().sorted().toList(), names);
    }
}
