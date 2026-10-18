/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.user.cellview.client;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.user.cellview.client.DataGrid.Resources;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

import java.util.Arrays;

/**
 * Tests for {@link DataGrid}.
 */
public class DataGridTest extends AbstractCellTableTestBase<DataGrid<String>> {
  
  /**
   * Test if the dataGridWidget style is applied.
   */
  public void testDataGridWidgetStyle() {
    Resources res = GWT.create(Resources.class); 
    DataGrid<String> dataGrid = new DataGrid<String>(20, res);
    
    String dataGridWidgetStyle = res.dataGridStyle().dataGridWidget();
    
    TableElement tableElem = dataGrid.getElement().cast();
    assertTrue(tableElem.getClassName().contains(dataGridWidgetStyle));
  }
  
  /**
   * Test that if a header builder does not add any rows, the header is hidden.
   */
  public void testHeaderBuilderEmpty() {
    DataGrid<String> table = createAbstractHasData();
    RootPanel.get().add(table);
    HeaderBuilder<String> emptyHeader = new AbstractHeaderOrFooterBuilder<String>(table, false) {
      @Override
      protected boolean buildHeaderOrFooterImpl() {
        return false;
      }
    };
    HeaderBuilder<String> notEmptyHeader = new AbstractHeaderOrFooterBuilder<String>(table, false) {
      @Override
      protected boolean buildHeaderOrFooterImpl() {
        return true;
      }
    };
    FooterBuilder<String> emptyFooter = new AbstractHeaderOrFooterBuilder<String>(table, true) {
      @Override
      protected boolean buildHeaderOrFooterImpl() {
        return false;
      }
    };
    FooterBuilder<String> notEmptyFooter = new AbstractHeaderOrFooterBuilder<String>(table, true) {
      @Override
      protected boolean buildHeaderOrFooterImpl() {
        return true;
      }
    };

    // Header is empty, footer is not.
    table.setHeaderBuilder(emptyHeader);
    table.setFooterBuilder(notEmptyFooter);
    table.getPresenter().flush();
    assertFalse(table.tableHeader.isAttached());
    assertTrue(table.tableFooter.isAttached());

    // Footer is empty, header is not.
    table.setHeaderBuilder(notEmptyHeader);
    table.setFooterBuilder(emptyFooter);
    table.getPresenter().flush();
    assertTrue(table.tableHeader.isAttached());
    assertFalse(table.tableFooter.isAttached());

    RootPanel.get().remove(table);
  }

  /**
   * Test that the header, body and footer tables read as one ARIA table.
   */
  public void testTableRoles() {
    DataGrid<String> table = createRoleTestGrid();

    assertEquals("table", table.getElement().getAttribute("role"));
    assertEquals("presentation", table.tableHeader.getElement().getAttribute("role"));
    assertEquals("presentation", table.tableData.getElement().getAttribute("role"));
    assertEquals("presentation", table.tableFooter.getElement().getAttribute("role"));
    assertEquals("rowgroup", table.getTableHeadElement().getAttribute("role"));
    assertEquals("rowgroup", table.getTableBodyElement().getAttribute("role"));
    assertEquals("rowgroup", table.getTableFootElement().getAttribute("role"));

    TableRowElement headerRow = table.getTableHeadElement().getRows().getItem(0);
    assertEquals("row", headerRow.getAttribute("role"));
    for (int c = 0; c < 3; c++) {
      assertEquals("columnheader", headerRow.getCells().getItem(c).getAttribute("role"));
    }
    for (int r = 0; r < 3; r++) {
      TableRowElement row = table.getRowElement(r);
      assertEquals("row", row.getAttribute("role"));
      for (int c = 0; c < 3; c++) {
        assertEquals("cell", row.getCells().getItem(c).getAttribute("role"));
      }
    }

    RootPanel.get().remove(table);
  }

  /**
   * Test that rows replaced by a partial redraw get roles too.
   */
  public void testTableRolesAfterPartialRedraw() {
    DataGrid<String> table = createRoleTestGrid();

    table.setRowData(1, Arrays.asList("B"));
    table.getPresenter().flush();
    TableRowElement row = table.getRowElement(1);
    assertEquals("B", row.getCells().getItem(0).getInnerText());
    assertEquals("row", row.getAttribute("role"));
    assertEquals("cell", row.getCells().getItem(1).getAttribute("role"));

    RootPanel.get().remove(table);
  }

  /**
   * Test that a row the builder marks presentational keeps that role and its
   * cells get none.
   */
  public void testTableRolesKeepExplicitRoles() {
    DataGrid<String> table = createRoleTestGrid();

    table.setTableBuilder(new DefaultCellTableBuilder<String>(table) {
      @Override
      public void buildRowImpl(String rowValue, int absRowIndex) {
        super.buildRowImpl(rowValue, absRowIndex);
        if (absRowIndex == 0) {
          TableRowBuilder tr = startRow(rowValue);
          tr.attribute("role", "presentation");
          tr.startTD().colSpan(3).endTD();
          tr.endTR();
        }
      }
    });
    table.getPresenter().flush();
    TableRowElement layoutRow = table.getTableBodyElement().getRows().getItem(1);
    assertEquals("presentation", layoutRow.getAttribute("role"));
    assertFalse(layoutRow.getCells().getItem(0).hasAttribute("role"));
    assertEquals("row", table.getTableBodyElement().getRows().getItem(2).getAttribute("role"));

    RootPanel.get().remove(table);
  }

  /**
   * Test that a sorted header carries both its role and aria-sort.
   */
  public void testTableRolesKeepAriaSort() {
    DataGrid<String> table = createRoleTestGrid();

    table.getColumnSortList().push(table.getColumn(2));
    table.redrawHeaders();
    TableCellElement th = table.getTableHeadElement().getRows().getItem(0).getCells().getItem(2);
    assertEquals("columnheader", th.getAttribute("role"));
    assertEquals("ascending", th.getAttribute("aria-sort"));

    RootPanel.get().remove(table);
  }

  /**
   * Test that the empty-table and loading containers do not read as tables.
   */
  public void testEmptyAndLoadingContainersArePresentational() {
    DataGrid<String> table = createAbstractHasData();
    table.setEmptyTableWidget(new Label("Empty"));
    table.setLoadingIndicator(new Label("Loading"));
    assertEquals("presentation",
        table.emptyTableWidgetContainer.getElement().getAttribute("role"));
    assertEquals("presentation",
        table.loadingIndicatorContainer.getElement().getAttribute("role"));
  }

  @Override
  protected DataGrid<String> createAbstractHasData() {
    return new DataGrid<String>();
  }

  @Override
  protected String getHeaderCellRole() {
    return "columnheader";
  }

  /**
   * Create an attached grid with three columns, the last one sortable, and
   * three rows.
   */
  private DataGrid<String> createRoleTestGrid() {
    DataGrid<String> table = createAbstractHasData(new TextCell());
    TextColumn<String> column = new TextColumn<String>() {
      @Override
      public String getValue(String object) {
        return object;
      }
    };
    column.setSortable(true);
    table.addColumn(column, new TextHeader("Name"));
    RootPanel.get().add(table);
    table.setRowData(Arrays.asList("a", "b", "c"));
    table.getPresenter().flush();
    return table;
  }

  @Override
  protected TableCellElement getBodyElement(DataGrid<String> table, int row, int column) {
    TableElement tableElem = table.tableData.getElement().cast();
    TableSectionElement tbody = tableElem.getTBodies().getItem(0);
    TableRowElement tr = tbody.getRows().getItem(row);
    return tr.getCells().getItem(column);
  }

  @Override
  protected int getHeaderCount(DataGrid<String> table) {
    TableElement tableElem = table.tableHeader.getElement().cast();
    TableSectionElement thead = tableElem.getTHead();
    if (thead.getRows().getLength() == 0) {
      return 0;
    }
    TableRowElement tr = thead.getRows().getItem(0);
    return tr.getCells().getLength();
  }

  @Override
  protected TableCellElement getHeaderElement(DataGrid<String> table, int column) {
    TableElement tableElem = table.tableHeader.getElement().cast();
    TableSectionElement thead = tableElem.getTHead();
    TableRowElement tr = thead.getRows().getItem(0);
    return tr.getCells().getItem(column);
  }
}
