package coldloops.scoreviewer;

import net.coderazzi.filters.IParser;

import javax.swing.*;
import java.text.ParseException;
import java.util.ArrayList;

public class CustomParser implements IParser {

    private final IParser parser;

    public CustomParser(IParser parser) {
        this.parser = parser;
    }

    @Override
    public RowFilter parseText(String s) throws ParseException {
        if(! s.contains("&")) return parser.parseText(s);

        String  [] parts = s.split("&");
        ArrayList<RowFilter<Object,Object>> l = new ArrayList<>();
        for(String p : parts) {
            l.add(parser.parseText(p));
        }
        return RowFilter.andFilter(l);
    }

    @Override
    public InstantFilter parseInstantText(String s) throws ParseException {
        return parser.parseInstantText(s);
    }

    @Override
    public String escape(String s) {
        return parser.escape(s);
    }

    @Override
    public String stripHtml(String s) {
        return parser.stripHtml(s);
    }
}
