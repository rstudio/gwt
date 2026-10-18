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
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.user.cellview.client.DataGrid.Resources;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

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
   * Test that a partial redraw only stamps the rows it inserts, so updating
   * one row doesn't walk the whole body.
   */
  public void testPartialRedrawOnlyStampsNewRows() {
    DataGrid<String> table = createRoleTestGrid();

    table.getRowElement(0).removeAttribute("role");
    table.setRowData(1, Arrays.asList("B"));
    table.getPresenter().flush();
    assertFalse(table.getRowElement(0).hasAttribute("role"));
    assertEquals("row", table.getRowElement(1).getAttribute("role"));

    RootPanel.get().remove(table);
  }

  /**
   * Test that an empty role attribute counts as no role.
   */
  public void testTableRolesTreatEmptyRoleAsAbsent() {
    DataGrid<String> table = createRoleTestGrid();

    table.setTableBuilder(new DefaultCellTableBuilder<String>(table) {
      @Override
      public void buildRowImpl(String rowValue, int absRowIndex) {
        TableRowBuilder tr = startRow(rowValue);
        tr.attribute("role", "");
        tr.startTD().attribute("role", "").text(rowValue).endTD();
        tr.endTR();
      }
    });
    table.getPresenter().flush();
    TableRowElement row = table.getRowElement(0);
    assertEquals("row", row.getAttribute("role"));
    assertEquals("cell", row.getCells().getItem(0).getAttribute("role"));

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
   * Test that a th in the body is a row header.
   */
  public void testTableRolesBodyThIsRowHeader() {
    DataGrid<String> table = createRoleTestGrid();

    table.setTableBuilder(new DefaultCellTableBuilder<String>(table) {
      @Override
      public void buildRowImpl(String rowValue, int absRowIndex) {
        TableRowBuilder tr = startRow(rowValue);
        tr.startTH().text(rowValue).endTH();
        tr.startTD().text(rowValue).endTD();
        tr.endTR();
      }
    });
    table.getPresenter().flush();
    TableRowElement row = table.getRowElement(0);
    assertEquals("rowheader", row.getCells().getItem(0).getAttribute("role"));
    assertEquals("cell", row.getCells().getItem(1).getAttribute("role"));

    RootPanel.get().remove(table);
  }

  /**
   * Test that the loading and empty-table messages, which replace the body
   * table, read as a row of the grid rather than a table of their own.
   */
  public void testLoadingAndEmptyMessagesArePresentational() {
    DataGrid<String> table = createAbstractHasData(new TextCell());
    Label loading = new Label("Loading");
    Label empty = new Label("Empty");
    table.setLoadingIndicator(loading);
    table.setEmptyTableWidget(empty);

    // A grid with no data yet is loading.
    RootPanel.get().add(table);
    table.getPresenter().flush();
    assertTrue(loading.isAttached());
    assertMessageRoles(table, loading);

    table.setRowData(new ArrayList<String>());
    table.getPresenter().flush();
    assertTrue(empty.isAttached());
    assertMessageRoles(table, empty);

    RootPanel.get().remove(table);
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
   * Assert that a message widget shown in the grid sits in a cell of a row of
   * a presentational table inside the grid.
   */
  private static void assertMessageRoles(DataGrid<String> table, Widget message) {
    Element td = message.getElement().getParentElement();
    Element tr = td.getParentElement();
    Element tbody = tr.getParentElement();
    Element tableElem = tbody.getParentElement();
    assertEquals("td", td.getTagName().toLowerCase(Locale.ROOT));
    assertTrue(table.getElement().isOrHasChild(tableElem));
    assertEquals("cell", td.getAttribute("role"));
    assertEquals("row", tr.getAttribute("role"));
    assertEquals("rowgroup", tbody.getAttribute("role"));
    assertEquals("presentation", tableElem.getAttribute("role"));
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
