package com.qiniu.up;

import com.qiniu.utils.Utils;

public class UpOption {
    public String EntryUri;
    public String MimeType;
    public String CustomMeta;
    public int Crc32;
    public int Rotate;
	public String Params;

    public String toUri() {
		if ( ! Utils.IsStringValid(EntryUri)) {
			return null;
		}
		StringBuffer sb = new StringBuffer(Utils.EncodeUri(EntryUri));

		if (Utils.IsStringValid(MimeType)) {
			sb.append("/mimeType/");
			sb.append(Utils.EncodeUri(MimeType));
		}

		if (Utils.IsStringValid(CustomMeta)) {
			sb.append("/meta/");
			sb.append(Utils.EncodeUri(CustomMeta));
		}

		if (Crc32 != 0) {
			sb.append("/crc32/");
			sb.append(Crc32);
		}

		if (Rotate != 0) {
			sb.append("/rotate/");
			sb.append(Rotate);
		}

        return sb.toString();
    }
}
