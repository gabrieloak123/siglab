package br.ufrn.dimap.siglab.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Países oferecidos no filtro de busca de conferências e a regra de correspondência
 * com o campo {@code location} da Conference Partner.
 * <p>
 * A API não expõe filtro por país: a localização vem como {@code "Cidade, Região, País"}
 * (ex.: {@code "Krakow, Poland"}), então o filtro é aplicado sobre o último segmento,
 * comparando com sinônimos (USA/United States, UK/United Kingdom, Korea/South Korea etc.)
 * por palavra inteira, para evitar falsos positivos como "Romania" conter "Oman".
 * </p>
 */
public final class ConferenceCountries {

    private static final List<Country> ALL = build();

    private ConferenceCountries() {}

    /** Um país exibido no formulário e os nomes aceitos no campo {@code location}. */
    public record Country(String name, List<String> aliases) {}

    /** Nomes dos países na ordem exibida (alfabética). */
    public static List<String> names() {
        return ALL.stream().map(Country::name).toList();
    }

    /** Indica se o país informado consta na lista (por nome ou sinônimo). */
    public static boolean isKnown(String countryName) {
        return find(countryName) != null;
    }

    /**
     * Indica se a localização pertence ao país informado.
     *
     * @param location    localização como retornada pela API (ex.: "Krakow, Poland")
     * @param countryName nome (ou sinônimo) de um país da lista
     */
    public static boolean matches(String location, String countryName) {
        Country country = find(countryName);
        if (country == null || location == null || location.isBlank()) return false;
        String segment = normalize(lastSegment(location));
        return country.aliases().stream().anyMatch(alias -> wordContains(segment, alias));
    }

    private static Country find(String countryName) {
        if (countryName == null || countryName.isBlank()) return null;
        String normalized = normalize(countryName);
        return ALL.stream()
                .filter(country -> normalize(country.name()).equals(normalized)
                        || country.aliases().contains(normalized))
                .findFirst().orElse(null);
    }

    /** País é o último segmento separado por vírgula; sem vírgula, a localização inteira. */
    private static String lastSegment(String location) {
        int comma = location.lastIndexOf(',');
        return (comma >= 0 ? location.substring(comma + 1) : location).trim();
    }

    /** Verdadeiro quando {@code needle} aparece como palavra(s) inteira(s) em {@code hay}. */
    static boolean wordContains(String hay, String needle) {
        return hay.equals(needle)
                || hay.startsWith(needle + " ")
                || hay.endsWith(" " + needle)
                || hay.contains(" " + needle + " ");
    }

    static String normalize(String value) {
        String noAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
        return noAccents.replaceAll("\\s+", " ");
    }

    private static Country country(String name, String... aliases) {
        List<String> all = new ArrayList<>();
        all.add(normalize(name));
        for (String alias : aliases) all.add(normalize(alias));
        return new Country(name, List.copyOf(all));
    }

    private static List<Country> build() {
        List<Country> countries = new ArrayList<>(List.of(
                country("Argentina"),
                country("Australia"),
                country("Austria"),
                country("Belgium"),
                country("Brazil"),
                country("Bulgaria"),
                country("Canada"),
                country("Chile"),
                country("China"),
                country("Colombia"),
                country("Croatia"),
                country("Cyprus"),
                country("Czech Republic", "Czechia"),
                country("Denmark"),
                country("Ecuador"),
                country("Egypt"),
                country("Estonia"),
                country("Finland"),
                country("France"),
                country("Germany"),
                country("Greece"),
                country("Hong Kong"),
                country("Hungary"),
                country("Iceland"),
                country("India"),
                country("Indonesia"),
                country("Iran", "Islamic Republic of Iran"),
                country("Ireland"),
                country("Israel"),
                country("Italy"),
                country("Japan"),
                country("Jordan"),
                country("Kenya"),
                country("Latvia"),
                country("Lithuania"),
                country("Luxembourg"),
                country("Malaysia"),
                country("Maldives"),
                country("Malta"),
                country("Mauritius"),
                country("Mexico"),
                country("Morocco"),
                country("Netherlands", "Holland"),
                country("New Zealand"),
                country("Nigeria"),
                country("Norway"),
                country("Pakistan"),
                country("Peru"),
                country("Philippines"),
                country("Poland"),
                country("Portugal"),
                country("Qatar"),
                country("Romania"),
                country("Russia", "Russian Federation"),
                country("Saudi Arabia"),
                country("Serbia"),
                country("Singapore"),
                country("Slovakia"),
                country("Slovenia"),
                country("South Africa"),
                country("South Korea", "Korea", "Republic of Korea"),
                country("Spain"),
                country("Sri Lanka", "Srilanka"),
                country("Sweden"),
                country("Switzerland"),
                country("Taiwan"),
                country("Thailand"),
                country("Tunisia"),
                country("Turkey", "Turkiye"),
                country("United Arab Emirates", "UAE"),
                country("United Kingdom", "UK", "England", "Scotland", "Wales"),
                country("Ukraine"),
                country("United States", "USA", "United States of America"),
                country("Uruguay"),
                country("Vietnam", "Viet Nam")));
        countries.sort(Comparator.comparing(Country::name));
        return List.copyOf(countries);
    }
}
