package com.stardust.autojs.apkbuilder;

import android.util.Log;

import pxb.android.StringItem;
import pxb.android.axml.AxmlWriter;
import pxb.android.axml.NodeVisitor;

/**
 * AndroidManifest文件修改，用于修改xml元素
 *
 * @author TonyJiangWJ
 * @since 2023/8/16
 */
public class MutableAxmlWriter extends AxmlWriter {
    private final ManifestEditor manifestEditor;

    public MutableAxmlWriter(ManifestEditor manifestEditor) {
        this.manifestEditor = manifestEditor;
    }

    @Override
    public NodeVisitor child(String ns, String name) {
        AxmlWriter.NodeImpl first = new MutableAxmlWriter.MutableNodeImpl(ns, name);
        this.firsts.add(first);
        return first;
    }

    public class MutableNodeImpl extends AxmlWriter.NodeImpl {
        private boolean ignore;
        private final String name;

        MutableNodeImpl(String ns, String name) {
            super(ns, name);
            this.name = name;
        }

        @Override
        protected void onAttr(AxmlWriter.Attr a) {
            if ("uses-permission".equals(this.name) && "name".equals(a.name.data) && a.value instanceof StringItem) {
                if (manifestEditor.filterPermission(((StringItem) a.value).data)) {
                    ignore = true;
                }
            }
            manifestEditor.onAttr(a);
            super.onAttr(a);
        }


        @Override
        public NodeVisitor child(String ns, String name) {
            Log.d("MutableAxmlWriter", "child: " + ns + " name: " + name);
            AxmlWriter.NodeImpl child = new MutableAxmlWriter.MutableNodeImpl(ns, name);
            this.children.add(child);
            return child;
        }

        public boolean isIgnore() {
            return ignore;
        }
    }
}

