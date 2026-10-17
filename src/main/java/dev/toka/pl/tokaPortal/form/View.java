package dev.toka.pl.tokaPortal.form;

/** What one player chose for the lists: the words to look for, the order (0 default, 1 by name) and the items on a page (0 for the default). */
public record View(String query, int sort, int perPage) {
    public static final View DEFAULT = new View("", 0, 0);

    public View {
        query = query == null ? "" : query;
    }
}
