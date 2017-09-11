package coldloops.scoreviewer;


import net.coderazzi.filters.IParser;
import net.coderazzi.filters.gui.IFilterEditor;
import net.coderazzi.filters.gui.IParserModel;
import net.coderazzi.filters.gui.ParserModel;

import java.text.Format;
import java.util.Comparator;

public class CustomParserModel extends ParserModel {

    @Override
    public IParser createParser(Format fmt, Comparator cmp, Comparator strCmp, boolean ignoreCase, int modelIdx) {
        return new CustomParser(super.createParser(fmt,cmp,strCmp,ignoreCase,modelIdx));
    }
}
