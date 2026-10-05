package com.example.brazucalite;

import android.text.Html;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lê as bases remotas de catálogo usadas pelo add-on Brazuca Play.
 * Esta classe importa apenas metadados/listagens e URLs diretas presentes nas listas.
 */
public final class BrazucaSource {
    private BrazucaSource() {}

    // Endereços identificados no add-on plugin.video.BrazucaPlay.Matrix 2.1.4.
    public static final String MOVIES_URL =
            "https://gist.githubusercontent.com/skyrisk/5b87797329c7b46422565ffbaab3be7e/raw/page.xml";
    public static final String SERIES_URL =
            "https://gist.githubusercontent.com/skyrisk/16070347f20c87c72540f9f805b57a66/raw/SeriesBase";

    private static final Pattern ITEM = Pattern.compile("<item>(.*?)</item>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern CHANNEL = Pattern.compile("<channel>(.*?)</channel>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public static Result download() throws Exception {
        String movies = Net.get(MOVIES_URL);
        String series = Net.get(SERIES_URL);
        List<MediaItemModel> items = new ArrayList<>();
        parseMovies(movies, items);
        parseSeries(series, items);
        return new Result(items, sha256(movies + "\n---SERIES---\n" + series));
    }

    static void parseMovies(String xml, List<MediaItemModel> out) {
        Matcher m = ITEM.matcher(xml == null ? "" : xml);
        int n = 0;
        while (m.find()) {
            String block = m.group(1);
            String title = clean(tag(block, "title"));
            if (title.isEmpty()) continue;
            List<String> links = tags(block, "link");
            String external = tag(block, "externallink");
            if (!external.isEmpty()) links.add(0, external);
            String chosen = chooseLink(links);
            String poster = firstNonEmpty(tag(block, "thumbnail"), tag(block, "fanart"));
            String date = clean(tag(block, "date"));
            String year = findYear(title + " " + date);
            String genre = clean(tag(block, "genre"));
            String category = genre.isEmpty() ? "Filme" : "Filme • " + genre;
            out.add(new MediaItemModel("movie-" + (++n) + "-" + shortHash(title), title, year, category, poster, chosen));
        }
    }

    static void parseSeries(String xml, List<MediaItemModel> out) {
        Matcher m = CHANNEL.matcher(xml == null ? "" : xml);
        int n = 0;
        while (m.find()) {
            String block = m.group(1);
            String title = clean(tag(block, "name"));
            if (title.isEmpty()) continue;
            String link = tag(block, "externallink");
            String poster = firstNonEmpty(tag(block, "thumbnail"), tag(block, "fanart"));
            String date = clean(tag(block, "date"));
            String year = findYear(title + " " + date);
            String genre = clean(tag(block, "genre"));
            String category = genre.isEmpty() ? "Série" : "Série • " + genre;
            out.add(new MediaItemModel("series-" + (++n) + "-" + shortHash(title), title, year, category, poster, link));
        }
    }

    public static boolean isDirectPlayable(String url) {
        if (url == null) return false;
        String u = url.trim().toLowerCase(Locale.ROOT);
        if (!(u.startsWith("https://") || u.startsWith("http://"))) return false;
        // Media3 consegue lidar com URLs diretas progressivas/HLS/DASH.
        return u.contains(".m3u8") || u.contains(".mp4") || u.contains(".mpd") || u.contains(".webm") || u.contains(".m4v");
    }

    private static String chooseLink(List<String> links) {
        if (links == null || links.isEmpty()) return "";
        for (String s : links) if (isDirectPlayable(stripKodiHeaders(s))) return stripKodiHeaders(s);
        return stripKodiHeaders(links.get(0));
    }

    private static String stripKodiHeaders(String s) {
        if (s == null) return "";
        // Kodi permite URL|Header=...; o player Android recebe só a URL base nesta versão lite.
        int pipe = s.indexOf('|');
        return (pipe >= 0 ? s.substring(0, pipe) : s).trim();
    }

    private static String tag(String block, String name) {
        Matcher m = Pattern.compile("<" + Pattern.quote(name) + ">(.*?)</" + Pattern.quote(name) + ">",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(block == null ? "" : block);
        return m.find() ? decode(m.group(1).trim()) : "";
    }

    private static List<String> tags(String block, String name) {
        List<String> r = new ArrayList<>();
        Matcher m = Pattern.compile("<" + Pattern.quote(name) + ">(.*?)</" + Pattern.quote(name) + ">",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(block == null ? "" : block);
        while (m.find()) r.add(decode(m.group(1).trim()));
        return r;
    }

    private static String clean(String s) {
        if (s == null) return "";
        String x = s
                .replaceAll("(?i)\\[/?B\\]", "")
                .replaceAll("(?i)\\[/?I\\]", "")
                .replaceAll("(?i)\\[COLOR(?: [^\\]]+)?\\]", "")
                .replaceAll("(?i)\\[/COLOR\\]", "")
                .replace("[CR]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return decode(x);
    }

    @SuppressWarnings("deprecation")
    private static String decode(String s) {
        try { return Html.fromHtml(s).toString(); }
        catch (Exception e) { return s == null ? "" : s; }
    }

    private static String findYear(String s) {
        Matcher m = Pattern.compile("(?:19|20)\\d{2}").matcher(s == null ? "" : s);
        return m.find() ? m.group() : "";
    }

    private static String firstNonEmpty(String a, String b) {
        return a != null && !a.trim().isEmpty() ? a.trim() : (b == null ? "" : b.trim());
    }

    private static String shortHash(String s) {
        return Integer.toHexString((s == null ? "" : s).hashCode());
    }

    private static String sha256(String s) throws Exception {
        MessageDigest d = MessageDigest.getInstance("SHA-256");
        byte[] b = d.digest(s.getBytes("UTF-8"));
        StringBuilder x = new StringBuilder();
        for (byte v : b) x.append(String.format(Locale.US, "%02x", v));
        return x.toString();
    }

    public static final class Result {
        public final List<MediaItemModel> items;
        public final String hash;
        Result(List<MediaItemModel> items, String hash) { this.items = items; this.hash = hash; }
    }
}
