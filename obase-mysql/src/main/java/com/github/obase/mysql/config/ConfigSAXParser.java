package com.github.obase.mysql.config;

import org.springframework.core.io.Resource;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.github.obase.MessageException;
import com.github.obase.kit.SAXKit;
import com.github.obase.mysql.MysqlErrno;

public class ConfigSAXParser extends DefaultHandler {

	static final String ELEM_MYSQL = "obase-mysql";
	static final String ELEM_TABLE = "table-class";
	static final String ELEM_META = "meta-class";
	static final String ELEM_SQL = "statement";

	static final String ATTR_NAMESPACE = "namespace";
	static final String ATTR_ID = "id";

	ConfigMetaInfo config;
	boolean skip;
	String id;
	final StringBuilder content = new StringBuilder(512);

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (ELEM_MYSQL.equals(localName)) {
			config = new ConfigMetaInfo(attributes.getValue(ATTR_NAMESPACE));
			skip = true;
		} else {
			id = attributes.getValue(ATTR_ID);
			skip = false;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		content.append(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (ELEM_TABLE.equals(localName)) {
			config.tables.add(filterWhiteSpaces(content));
			content.setLength(0);
		} else if (ELEM_META.equals(localName)) {
			config.metas.add(filterWhiteSpaces(content));
			content.setLength(0);
		} else if (ELEM_SQL.equals(localName)) {
			if (config.sqls.put(id, filterWhiteSpaces(content)) != null) {
				throw new MessageException(MysqlErrno.SOURCE, MysqlErrno.SQL_CONFIG_DUPLICATE, "Duplicate sql id: " + config.namespace + '.' + id);
			}
			content.setLength(0);
		}
	}

	public static ConfigMetaInfo parse(Resource rs) throws Exception {
		ConfigSAXParser handler = new ConfigSAXParser();
		if (SAXKit.parse(rs.getInputStream(), handler)) {
			return handler.config;
		}
		return null;
	}

	static String filterWhiteSpaces(StringBuilder sb) {
		int len = sb.length();
		int l = 0, h = len - 1;
		char ch = 0;
		while (l <= h && Character.isWhitespace(sb.charAt(l))) {
			l++;
		}
		while (h >= l && Character.isWhitespace(sb.charAt(h))) {
			h--;
		}
		for (int i = l; i <= h; i++) {
			ch = sb.charAt(i);
			if (ch == '\r' || ch == '\n') {
				sb.setCharAt(i, '\u0020');
				for (int j = i - 1; j > l && Character.isWhitespace(sb.charAt(j)); j--) {
					sb.setCharAt(j, '\0');
				}
				while ((++i) < h && Character.isWhitespace(sb.charAt(i))) {
					sb.setCharAt(i, '\0');
				}
			}
		}

		char[] ret = new char[h - l + 1];
		len = 0;
		while (l <= h) {
			if ((ch = sb.charAt(l++)) != '\0') {
				ret[len++] = ch;
			}
		}
		return len == 0 ? "" : new String(ret, 0, len);
	}
}
