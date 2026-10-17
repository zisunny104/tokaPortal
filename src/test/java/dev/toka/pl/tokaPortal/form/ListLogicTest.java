package dev.toka.pl.tokaPortal.form;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListLogicTest {
    private static final List<String> NAMES = List.of("Cherry", "apple", "Banana", "applesauce", "date");

    @Test
    void filterIgnoresCaseAndBlankQuery() {
        assertEquals(List.of("apple", "applesauce"), ListLogic.filter(NAMES, s -> s, "APPLE"));
        assertEquals(NAMES, ListLogic.filter(NAMES, s -> s, "  "));
        assertEquals(NAMES, ListLogic.filter(NAMES, s -> s, null));
    }

    @Test
    void sortByNameIsCaseInsensitiveAndDefaultKeepsOrder() {
        assertEquals(List.of("apple", "applesauce", "Banana", "Cherry", "date"), ListLogic.sort(NAMES, s -> s, 1));
        assertEquals(NAMES, ListLogic.sort(NAMES, s -> s, 0));
    }

    @Test
    void perPageStaysBetweenLimits() {
        assertEquals(10, ListLogic.perPage(0, 10, 3, 30));
        assertEquals(3, ListLogic.perPage(1, 10, 3, 30));
        assertEquals(30, ListLogic.perPage(99, 10, 3, 30));
        assertEquals(5, ListLogic.perPage(0, 2, 5, 3));
    }

    @Test
    void composeCutsPagesAndMovesPageIntoRange() {
        ListLogic.Result<String> r = ListLogic.compose(NAMES, s -> s, new View("", 1, 3), 10, 3, 30, 2);
        assertEquals(List.of("Cherry", "date"), r.shown());
        assertEquals(2, r.page());
        assertEquals(2, r.pages());
        assertEquals(5, r.matched());
        assertEquals(2, ListLogic.compose(NAMES, s -> s, new View("", 1, 3), 10, 3, 30, 9).page());
    }

    @Test
    void composeCountsMatchedAndTotal() {
        ListLogic.Result<String> r = ListLogic.compose(NAMES, s -> s, new View("zzz", 0, 0), 10, 3, 30, 1);
        assertEquals(0, r.matched());
        assertEquals(5, r.total());
        assertEquals(1, r.pages());
        assertEquals(List.of(), r.shown());
    }

    @Test
    void preferencesAreKeptUntilForgotten() {
        UUID id = UUID.randomUUID();
        assertEquals(View.DEFAULT, Preferences.get(id));
        Preferences.set(id, new View("a", 1, 7));
        assertEquals(7, Preferences.get(id).perPage());
        Preferences.forget(id);
        assertEquals(View.DEFAULT, Preferences.get(id));
    }

    @Test
    void textsFillValues() {
        FormTexts t = new FormTexts("", "", "", "", "", "n=${0}", "", "", "", "", "", "", "q=${0}", "${0} ${1}/${2}");
        assertEquals("n=4", t.optionsHintFor(4));
        assertEquals("q=x", t.searchNoneFor("x"));
        assertEquals("T 2/5", t.pageTitleFor("T", 2, 5));
    }
}
