/*
 * Copyright 2012-2016 JetBrains s.r.o
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.jetpad.projectional.cell.mapping;

import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.mapper.ByTargetIndex;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.mapper.MappingContext;
import jetbrains.jetpad.mapper.Synchronizer;
import jetbrains.jetpad.model.composite.HasParent;
import jetbrains.jetpad.model.composite.TreePath;
import jetbrains.jetpad.projectional.cell.ProjectionalRoleSynchronizer;

import java.util.*;

public final class CellProvider {

  public static CellProvider fromMapper(Mapper<?, ?> mapper) {
    MappingContext mappingContext = mapper.getMappingContext();
    if (!mappingContext.contains(ByTargetIndex.KEY)) {
      mappingContext.put(ByTargetIndex.KEY, new ByTargetIndex(mappingContext));
    }
    return new CellProvider(mapper);
  }

  private Mapper<?, ?> myRootMapper;
  private ByTargetIndex myIndex;

  private CellProvider(Mapper<?, ?> enclosingMapper) {
    myRootMapper = enclosingMapper;
    myIndex = enclosingMapper.getMappingContext().get(ByTargetIndex.KEY);
  }

  public List<Cell> getCells(final HasParent<?> source) {
    return getCells(new Iterator<Object>() {
      private HasParent<?> myCurrent = source;

      @Override
      public boolean hasNext() {
        return myCurrent != null;
      }

      @Override
      public Object next() {
        if (myCurrent == null) {
          throw new NoSuchElementException();
        }
        HasParent<?> current = myCurrent;
        myCurrent = myCurrent.getParent();
        return current;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    });
  }

  public List<Cell> getCells(Iterable<Object> sourcePath) {
    return getCells(sourcePath.iterator());
  }

  public List<Cell> getCells(Iterator<Object> sourcePath) {
    Object actualSource = sourcePath.next();
    return lookupCells(actualSource, actualSource, sourcePath);
  }

  public List<Cell> getCells(Object source) {
    return lookupCells(source, source, Collections.emptyIterator());
  }

  public Object getSource(Cell cell) {
    return lookupSource(cell);
  }

  private Object lookupSource(Cell cell) {
    for (Cell c = cell; c != null; c = c.getParent()) {
      Collection<Mapper<?, ?>> mappers = myIndex.getMappers(c);
      if (!mappers.isEmpty()) {
        Mapper<?, ?> mapper = mappers.iterator().next();
        Object source = doGetSource(mapper, cell);
        return source == null ? mapper.getSource() : source;
      }
    }
    return null;
  }

  private Object doGetSource(Mapper<?, ?> mapper, Cell actualTarget) {
    if (mapper.getTarget() == actualTarget) {
      return mapper.getSource();
    }
    for (Synchronizer sync : mapper.synchronizers()) {
      Object source = getSource(sync, actualTarget);
      if (source != null) return source;
    }
    return null;
  }

  private Object getSource(Synchronizer sync, Cell target) {
    if (sync instanceof ToCellMapping) {
      return ((ToCellMapping) sync).getSource(target);
    }
    if (sync instanceof ProjectionalRoleSynchronizer) {
      return getSourceFromProjectionalSynchronizer((ProjectionalRoleSynchronizer<?, ?>) sync, target);
    }
    return null;
  }

  private Object getSourceFromProjectionalSynchronizer(ProjectionalRoleSynchronizer<?, ?> sync, Cell cell) {
    for (Mapper<?, ? extends Cell> mapper : sync.getMappers()) {
      Object source = doGetSource(mapper, cell);
      if (source != null) return source;
    }
    return null;
  }

  private List<Cell> lookupCells(Object source, Object actualSource, Iterator<Object> parents) {
    Set<Mapper<? super Object, ?>> mappers = myRootMapper.getMappingContext().getMappers(myRootMapper, source);
    if (!mappers.isEmpty()) {
      List<Cell> cells = new ArrayList<>(mappers.size());
      for (Mapper<?, ?> mapper : mappers) {
        if (mapper.getTarget() instanceof Cell) {
          cells.addAll(doGetCells((Mapper<?, ? extends Cell>) mapper, actualSource));
        }
      }
      TreePath.sort(cells);
      return cells;
    }
    if (parents.hasNext()) {
      return lookupCells(parents.next(), actualSource, parents);
    }
    return Collections.emptyList();
  }

  private List<Cell> doGetCells(Mapper<?, ? extends Cell> mapper, Object actualSource) {
    if (mapper.getSource() == actualSource) {
      return Collections.<Cell>singletonList(mapper.getTarget());
    }
    List<Cell> result = null;
    for (Synchronizer sync : mapper.synchronizers()) {
      List<Cell> cells = getCells(sync, actualSource);
      if (cells != null && !cells.isEmpty()) {
        if (result == null) {
          result = new ArrayList<>(cells.size());
        }
        result.addAll(cells);
      }
    }
    return result == null ? Collections.<Cell>emptyList() : result;
  }

  private List<Cell> getCells(Synchronizer sync, Object source) {
    if (sync instanceof ToCellMapping) {
      return ((ToCellMapping) sync).getCells(source);
    }
    if (sync instanceof ProjectionalRoleSynchronizer) {
      return getCellsFromProjectionalSynchronizer((ProjectionalRoleSynchronizer<?, ?>) sync, source);
    }
    return null;
  }

  private List<Cell> getCellsFromProjectionalSynchronizer(ProjectionalRoleSynchronizer<?, ?> sync, Object source) {
    List<Cell> result = null;
    for (Mapper<?, ? extends Cell> mapper : sync.getMappers()) {
      List<Cell> cells = doGetCells(mapper, source);
      if (!cells.isEmpty()) {
        if (result == null) {
          result = new ArrayList<>(cells.size());
        }
        result.addAll(cells);
      }
    }
    return result;
  }
}
