/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.changedetection.state;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.gradle.api.Nullable;
import org.gradle.api.internal.changedetection.rules.TaskStateChange;
import org.gradle.api.internal.tasks.cache.TaskCacheKeyBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class FileCollectionSnapshotImpl implements FileCollectionSnapshot, FilesSnapshotSet {
    final Map<String, IncrementalFileSnapshot> snapshots;
    final List<TreeSnapshot> treeSnapshots;
    final TaskFilePropertyCompareType compareType;

    public FileCollectionSnapshotImpl(List<TreeSnapshot> treeSnapshots, TaskFilePropertyCompareType compareType) {
        this(convertTreeSnapshots(treeSnapshots), ImmutableList.copyOf(treeSnapshots), compareType);
    }

    public FileCollectionSnapshotImpl(Map<String, IncrementalFileSnapshot> snapshots, TaskFilePropertyCompareType compareType) {
        this(snapshots, null, compareType);
    }

    private FileCollectionSnapshotImpl(Map<String, IncrementalFileSnapshot> snapshots, List<TreeSnapshot> treeSnapshots, TaskFilePropertyCompareType compareType) {
        this.snapshots = snapshots;
        this.treeSnapshots = treeSnapshots;
        this.compareType = compareType;
    }

    private static Map<String, IncrementalFileSnapshot> convertTreeSnapshots(List<TreeSnapshot> treeSnapshots) {
        Map<String, IncrementalFileSnapshot> snapshots = Maps.newLinkedHashMap();
        for (TreeSnapshot treeSnapshot : treeSnapshots) {
            for(FileSnapshotWithKey fileSnapshotWithKey : treeSnapshot.getFileSnapshots()) {
                snapshots.put(fileSnapshotWithKey.getKey(), fileSnapshotWithKey.getIncrementalFileSnapshot());
            }
        }
        return snapshots;
    }

    public List<File> getFiles() {
        List<File> files = Lists.newArrayList();
        for (Map.Entry<String, IncrementalFileSnapshot> entry : snapshots.entrySet()) {
            if (!(entry.getValue() instanceof DirSnapshot)) {
                files.add(new File(entry.getKey()));
            }
        }
        return files;
    }

    @Override
    public Map<String, IncrementalFileSnapshot> getSnapshots() {
        return snapshots;
    }

    @Nullable
    @Override
    public FileSnapshot findSnapshot(File file) {
        IncrementalFileSnapshot s = snapshots.get(file.getAbsolutePath());
        if (s instanceof FileHashSnapshot) {
            return s;
        }
        return null;
    }

    @Override
    public FilesSnapshotSet getSnapshot() {
        return this;
    }

    @Override
    public Collection<Long> getTreeSnapshotIds() {
        List<Long> snapshotIds = new ArrayList<Long>();
        if (treeSnapshots != null) {
            for (TreeSnapshot treeSnapshot : treeSnapshots) {
                if (treeSnapshot.isShareable() && treeSnapshot.getAssignedId() != null && treeSnapshot.getAssignedId() != -1L) {
                    snapshotIds.add(treeSnapshot.getAssignedId());
                }
            }
        }
        return snapshotIds;
    }

    @Override
    public boolean isEmpty() {
        return snapshots.isEmpty();
    }

    @Override
    public Iterator<TaskStateChange> iterateContentChangesSince(FileCollectionSnapshot oldSnapshot, String fileType) {
        return compareType.iterateContentChangesSince(snapshots, oldSnapshot.getSnapshots(), fileType);
    }

    @Override
    public void appendToCacheKey(TaskCacheKeyBuilder builder) {
        compareType.appendToCacheKey(builder, snapshots);
    }
}
