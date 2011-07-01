package org.scalastuff.osgitools;

import static org.scalastuff.osgitools.util.StringTokenizer.tokenize;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class OsgiManifest {
	
	private final Map<String, Attribute> attributes = new LinkedHashMap<String, OsgiManifest.Attribute>();
	
	public Set<String> getAttributeNames() {
		return attributes.keySet();
	}
	
	public Attribute getAttribute(String name) {
		Attribute values = attributes.get(name);
		if (values == null) {
			values = new Attribute();
			attributes.put(name, values);
		}
		return values;
	}

	public String toString() {
		return toString(",", new StringBuilder()).toString();
	}
	
	public StringBuilder toString(String sep, StringBuilder out) {
		for (Entry<String, Attribute> attr : attributes.entrySet()) {
			out.append(attr.getKey()).append(": ");
			attr.getValue().toString(sep, out);
			out.append("\n");
		}
		return out;
	}
	
	public static class Attribute extends ArrayList<OsgiManifest.Value> {
		
		private static final long serialVersionUID = 1L;

		public boolean isSet() {
			return !super.isEmpty();
		}
		
		public void addValues(Value... values) {
			super.addAll(Arrays.asList(values));
		}
		
		public Value findValue(String stringValue) {
			for (Value value : this) {
				if (value.getValue().equals(stringValue)) {
					return value;
				}
			}
			return null;
		}

		public void removeValue(String string) {
			remove(findValue(string));
		}
		
		public Value addUnique(String stringValue) {
			for (Value value : this) {
				if (value.getValue().equals(stringValue)) {
					return value;
				}
			}
			Value value = new Value();
			value.setValue(stringValue);
			add(value);
			return value;
		}
		
		public Value setValue(String stringValue, boolean overwriteExisting) {
			if (overwriteExisting || isEmpty()) {
				clear();
				return setValue(stringValue);
			}
			return get(0);
		}
		
		public Value setValue(String stringValue) {
			if (isEmpty()) {
				add(new Value());
			} else {
				super.removeRange(1, size());
			}
			get(0).setValue(stringValue);
			return get(0);
		}
		
		@Override
		public String toString() {
			return toString(",", new StringBuilder()).toString();
		}
		
		public StringBuilder toString(String sep, StringBuilder out) {
			for (int i = 0; i < size(); i++) {
				if (i > 0) out.append(sep);
				out.append(get(i));
			}
			return out;
		}
	}
	
	public static class Value {
		private String value;
		private Map<String, String> annotations = new LinkedHashMap<String, String>();
		
		public String getValue() {
			return value;
		}
		
		public Value setValue(String value) {
			this.value = value;
			return this;
		}
		
		public Set<String> getAnnotationNames() {
			return annotations.keySet();
		}
		
		public String getAnnotation(String annotationName) {
			String annotation = annotations.get(annotationName);
			if (annotation == null) {
				annotation = "";
			}
			return annotation;
		}
		
		public void setAnnotation(String annotationName, String annotation) {
			if (annotation.equals("")) {
				annotations.remove(annotationName);
			} else {
				annotations.put(annotationName, annotation);
			}
		}
		
		@Override
		public String toString() {
			return toString(new StringBuilder()).toString();
		}
		
		public StringBuilder toString(StringBuilder out) {
			out.append(value);
			for (Entry<String, String> annotation : annotations.entrySet()) {
				out.append(';').append(annotation.getKey()).append("=").append(annotation.getValue());
			}
			return out;
		}
	}
	
	public static OsgiManifest read(BufferedReader reader) throws IOException {
		try {
			List<String> lines = new ArrayList<String>();
 			for (String line = ""; line != null; line = reader.readLine()) {
 				lines.add(line);
			}
 			return read(lines);
		} finally {
			reader.close();
		}
	}
		
	public static OsgiManifest read(List<String> lines) {
		
		// parse lines
		OsgiManifest manifest = new OsgiManifest();
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			for (int j = i + 1; j < lines.size() && lines.get(j).startsWith(" "); j++, i++) {
				line = line + lines.get(j);
			}
			int index = line.indexOf(':');
			if (index > 0) {
				String attributeName = line.substring(0, index).trim();
				List<String> valueTokens = tokenize(line.substring(index + 1), ',');
				Attribute values = manifest.getAttribute(attributeName);
				for (String valueToken : valueTokens) {
					if (valueToken.trim().isEmpty()) continue;
					Value value = new Value();
					values.add(value);
					List<String> annotationTokens = tokenize(valueToken, ';');
					value.setValue(annotationTokens.get(0));
					for (int k = 1; k < annotationTokens.size(); k++) {
						String annotationToken = annotationTokens.get(k);
						index = annotationToken.indexOf('=');
						if (index > 0) {
							value.setAnnotation(annotationToken.substring(0, index), annotationToken.substring(index + 1));
						}
					}
				}
			}
		}
		return manifest;
	}
	
	public List<String> write() {
		List<String> lines = new ArrayList<String>();
		StringBuilder out = new StringBuilder();
		for (Entry<String, Attribute> attribute : attributes.entrySet()) {
			out.append(attribute.getKey()).append(": ");
			Attribute attributeValue = attribute.getValue();
			if (attributeValue.size() > 1) {
				lines.add(out.toString());
				out.setLength(0);
				for (int i = 0; i < attributeValue.size(); i++) {
					Value value = attributeValue.get(i);
					out.append(" ");
					value.toString(out);
					if (i + 1 < attributeValue.size()) {
						out.append(",");
					}
					lines.add(out.toString());
					out.setLength(0);
				}
			} else {
				attributeValue.toString("", out);
				lines.add(out.toString());
				out.setLength(0);
			}
		}
		return lines;
	}
}
