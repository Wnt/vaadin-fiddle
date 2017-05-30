package org.vaadin.vaadinfiddle.vaadinfiddleprototype.util;
import java.io.File;
import java.util.stream.Stream;

import com.vaadin.data.provider.AbstractHierarchicalDataProvider;
import com.vaadin.data.provider.HierarchicalQuery;

public class FileSystemProvider
        extends AbstractHierarchicalDataProvider<File, Void> {

    private final File root;

    public FileSystemProvider(File root) {
        this.root = root;
    }

    @Override
    public int getChildCount(HierarchicalQuery<File, Void> query) {
        return getParent(query).list().length;
    }

    private File getParent(HierarchicalQuery<File, Void> query) {
        File parent = query.getParent();
        return parent != null ? parent : root;
    }

    @Override
    public Stream<File> fetchChildren(HierarchicalQuery<File, Void> query) {
        File[] children = getParent(query).listFiles();
        return Stream.of(children).sorted().skip(query.getOffset())
                .limit(query.getLimit());
    }

    @Override
    public boolean hasChildren(File item) {
        return item.isDirectory() && item.listFiles().length > 0;
    }

    @Override
    public boolean isInMemory() {
        return false;
    }

}