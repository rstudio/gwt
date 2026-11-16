/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.user.client.ui;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle.MultiWordSuggestion;
import com.google.gwt.user.client.ui.SuggestBox.DefaultSuggestionDisplay;
import com.google.gwt.user.client.ui.SuggestBox.SuggestionCallback;
import com.google.gwt.user.client.ui.SuggestBox.SuggestionDisplay;
import com.google.gwt.user.client.ui.SuggestOracle.Response;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link SuggestBoxTest}.
 */
public class SuggestBoxTest extends WidgetTestBase {

  /**
   * A SuggestOracle used for testing.
   */
  private static class TestOracle extends SuggestOracle {
    private Request request;
    private Callback callback;

    @Override
    public void requestSuggestions(Request request, Callback callback) {
      this.request = request;
      this.callback = callback;
    }
  }

  /**
   * A SuggestionDisplay used for testing.
   */
  private static class TestSuggestionDisplay extends DefaultSuggestionDisplay {

    private List<? extends Suggestion> suggestions;

    @Override
    protected void showSuggestions(SuggestBox suggestBox,
        Collection<? extends Suggestion> suggestions,
        boolean isDisplayStringHTML, boolean isAutoSelectEnabled,
        SuggestionCallback callback) {
      super.showSuggestions(suggestBox, suggestions, isDisplayStringHTML,
          isAutoSelectEnabled, callback);
      this.suggestions = new ArrayList<Suggestion>(suggestions);
    }

    /**
     * Get the suggestion at the specified index.
     *
     * @param index the index
     * @return the {@link Suggestion} at the index
     */
    public Suggestion getSuggestion(int index) {
      return suggestions.get(index);
    }

    /**
     * Get the number of suggestions that are currently showing. Used for
     * testing.
     *
     * @return the number of suggestions currently showing, 0 if there are none
     */
    public int getSuggestionCount() {
      return suggestions.size();
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  /**
   * Test the basic accessors.
   */
  @SuppressWarnings("deprecation")
  public void testAccessors() {
    SuggestBox box = createSuggestBox();

    // setLimit
    box.setLimit(5);
    assertEquals(5, box.getLimit());

    // setSelectsFirstItem
    box.setAutoSelectEnabled(true);
    assertTrue(box.isAutoSelectEnabled());
    box.setAutoSelectEnabled(false);
    assertFalse(box.isAutoSelectEnabled());

    // isSuggestionListShowing
    assertFalse(box.isSuggestionListShowing());
    box.showSuggestions("test");
    assertTrue(box.isSuggestionListShowing());
  }

  public void testGettextShouldBeCalledWhenOverrided() {

    // Verify that the query matches the overridden getText.
    SuggestOracle oracle = new SuggestOracle() {
      @Override
      public void requestSuggestions(Request request, Callback callback) {
        if ("override".equals(request.getQuery())) {
          finishTest();
        } else {
          fail("Expected query: override");
        }
      }
    };

    // Create a customized SuggestBox which overrides getText.
    SuggestBox box = new SuggestBox(oracle) {
      @Override
      public String getText() {
        return "override";
      }
    };

    // Attach the box.
    RootPanel.get().add(box);

    // showSuggestionList should call the overridden method.
    delayTestFinish(1000);
    box.showSuggestionList();
  }

  public void testLargerMatchShows() {
    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle(" ");
    oracle.add("He'll help me wont he");

    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = new SuggestBox(oracle, new TextBox(), display);
    RootPanel.get().add(box);
    box.setText("He help");
    box.showSuggestionList();
    assertTrue(display.isSuggestionListShowing());
    assertEquals(1, display.getSuggestionCount());
    assertEquals("<strong>He</strong>&#39;ll <strong>help</strong> me wont <strong>he</strong>",
        display.getSuggestion(0).getDisplayString());
  }

  public void testMultipleWordMatchesShow() {
    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle(",! ");
    oracle.add("Hark, Shark and Herald");
    oracle.add("Hark! The Herald Angels Sing");
    oracle.add("Heraldings and Harkings");
    oracle.add("Send my regards to Herald");

    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = new SuggestBox(oracle, new TextBox(), display);
    RootPanel.get().add(box);
    box.setText("Herald! Hark");
    box.showSuggestionList();
    assertTrue(display.isSuggestionListShowing());
    assertEquals(3, display.getSuggestionCount());
    assertEquals("<strong>Hark</strong>, Shark and <strong>Herald</strong>",
        display.getSuggestion(0).getDisplayString());
    assertEquals("<strong>Hark</strong>! The <strong>Herald</strong> Angels Sing",
        display.getSuggestion(1).getDisplayString());
    assertEquals("<strong>Herald</strong>ings and <strong>Hark</strong>ings",
        display.getSuggestion(2).getDisplayString());
  }

  public void testMatchCustomSort() {
    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle(",! ");
    oracle.add("Hark, Shark and Herald");
    oracle.add("Hark! The Herald Angels Sing");
    oracle.add("Heraldings and Harkings");
    oracle.add("Send my regards to Herald");
    oracle.setComparator(Collections.<String>reverseOrder());

    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = new SuggestBox(oracle, new TextBox(), display);
    RootPanel.get().add(box);
    box.setText("Herald! Hark");
    box.showSuggestionList();
    assertTrue(display.isSuggestionListShowing());
    assertEquals(3, display.getSuggestionCount());
    assertEquals("<strong>Herald</strong>ings and <strong>Hark</strong>ings",
        display.getSuggestion(0).getDisplayString());
    assertEquals("<strong>Hark</strong>! The <strong>Herald</strong> Angels Sing",
        display.getSuggestion(1).getDisplayString());
    assertEquals("<strong>Hark</strong>, Shark and <strong>Herald</strong>",
        display.getSuggestion(2).getDisplayString());
  }

  public void testMultipleSuggestionsWithSameNormalizedCandidateDefault() {
    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
    oracle.add("Mobile");
    oracle.add("MOBILE");
    oracle.add("MoBiLe");
    oracle.add("mobile");

    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = new SuggestBox(oracle, new TextBox(), display);
    RootPanel.get().add(box);
    box.setText("m");
    box.showSuggestionList();
    assertTrue(display.isSuggestionListShowing());
    assertEquals(1, display.getSuggestionCount());
    assertEquals("<strong>m</strong>obile",
        display.getSuggestion(0).getDisplayString());
  }

  public void testMultipleSuggestionsWithSameNormalizedCandidateSuggestAllMatchingWords() {
    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
    oracle.add("Mobile");
    oracle.add("MOBILE");
    oracle.add("MoBiLe");
    oracle.add("mobile");
    oracle.setSuggestAllMatchingWords(true);

    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = new SuggestBox(oracle, new TextBox(), display);
    RootPanel.get().add(box);
    box.setText("m");
    box.showSuggestionList();
    assertTrue(display.isSuggestionListShowing());
    assertEquals(4, display.getSuggestionCount());
    assertEquals("<strong>M</strong>OBILE",
        display.getSuggestion(0).getDisplayString());
    assertEquals("<strong>M</strong>oBiLe",
        display.getSuggestion(1).getDisplayString());
    assertEquals("<strong>M</strong>obile",
        display.getSuggestion(2).getDisplayString());
    assertEquals("<strong>m</strong>obile",
        display.getSuggestion(3).getDisplayString());
  }

  @SuppressWarnings("deprecation")
  public void testShowAndHide() {
    SuggestBox box = createSuggestBox();
    TestSuggestionDisplay display = (TestSuggestionDisplay) box.getSuggestionDisplay();
    assertFalse(display.isSuggestionListShowing());

    // should do nothing, box is not attached.
    box.showSuggestionList();
    assertFalse(display.isSuggestionListShowing());

    // Adds the suggest box to the root panel.
    RootPanel.get().add(box);
    assertFalse(display.isSuggestionListShowing());

    // Hides the list of suggestions, should be a no-op.
    box.hideSuggestionList();

    // Should try to show, but still fail, as there are no default suggestions.
    box.showSuggestionList();
    assertFalse(display.isSuggestionListShowing());

    // Now, finally, should be true
    box.setText("t");
    box.showSuggestionList();
    assertTrue(display.isSuggestionListShowing());

    // Hides it for real this time.
    box.hideSuggestionList();
    assertFalse(display.isSuggestionListShowing());
  }

  public void testEnabled() {
    SuggestBox box = createSuggestBox();
    TestSuggestionDisplay display = (TestSuggestionDisplay) box.getSuggestionDisplay();
    assertFalse(display.isSuggestionListShowing());

    // Adds suggest box with suggestion showing
    RootPanel.get().add(box);
    box.setText("t");
    box.showSuggestionList();
    assertTrue(display.isSuggestionListShowing());

    // When the box is disabled
    box.setEnabled(false);

    // The suggestion list was hidden and the inner text box disabled
    assertFalse(display.isSuggestionListShowing());
    assertFalse(box.getTextBox().isEnabled());

    // When the box is re-enabled
    box.setEnabled(true);

    // The box is re-enabled, but the suggestion list not re-shown
    assertTrue(box.getTextBox().isEnabled());
    assertFalse(display.isSuggestionListShowing());
  }

  public void testDisabledIgnoresLateResponses() {
    TestOracle oracle = new TestOracle();
    SuggestBox box = new SuggestBox(oracle, new TextBox(), new TestSuggestionDisplay());
    TestSuggestionDisplay display = (TestSuggestionDisplay) box.getSuggestionDisplay();
    assertFalse(display.isSuggestionListShowing());

    // Adds suggest box with suggestion showing
    RootPanel.get().add(box);
    box.setText("t");
    box.showSuggestionList();

    // Waiting for response
    assertFalse(display.isSuggestionListShowing());

    // The box becomes disabled
    box.setEnabled(false);

    // Response comes back after that
    Collection<MultiWordSuggestion> suggestions = new ArrayList<MultiWordSuggestion>();
    suggestions.add(new MultiWordSuggestion("one", "one"));
    oracle.callback.onSuggestionsReady(oracle.request, new Response(suggestions));

    // The suggestion list stays hidden
    assertFalse(display.isSuggestionListShowing());
  }

  public void testDefaults() {
    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
    oracle.setDefaultSuggestionsFromText(Arrays.asList("A", "B"));
    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = new SuggestBox(oracle, new TextBox(), display);
    RootPanel.get().add(box);
    box.showSuggestionList();
    assertTrue(display.isSuggestionListShowing());
    assertEquals(2, display.getSuggestionCount());
    assertEquals("A", display.getSuggestion(0).getReplacementString());
    assertEquals("B", display.getSuggestion(1).getReplacementString());
  }

  public void testSuggestionSelection() {
    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
    oracle.setDefaultSuggestionsFromText(Arrays.asList("A", "B"));
    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = new SuggestBox(oracle, new TextBox(), display);
    box.setAutoSelectEnabled(false);
    RootPanel.get().add(box);
    box.showSuggestionList();

    // If nothing is selected, moving down will select the first item
    assertNull(display.getCurrentSelection());
    display.moveSelectionDown();
    assertEquals("A", display.getCurrentSelection().getReplacementString());

    // Once something is selected, selections are made as expected, but we do
    // not move outside the box
    display.moveSelectionDown();
    assertEquals("B", display.getCurrentSelection().getReplacementString());
    display.moveSelectionDown();
    assertEquals("B", display.getCurrentSelection().getReplacementString());
    display.moveSelectionUp();
    assertEquals("A", display.getCurrentSelection().getReplacementString());
    display.moveSelectionUp();
    assertEquals("A", display.getCurrentSelection().getReplacementString());

    // Reset the suggestions so that nothing is selected again
    display.hideSuggestions();
    box.showSuggestionList();
    assertNull(display.getCurrentSelection());

    // If nothing is selected, moving up will select the last item
    display.moveSelectionUp();
    assertEquals("B", display.getCurrentSelection().getReplacementString());
  }

  public void testComboboxRoles() {
    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = createDefaultsBox(display);
    Element input = box.getElement();
    assertEquals("combobox", input.getAttribute("role"));
    assertEquals("list", input.getAttribute("aria-autocomplete"));
    assertEquals("false", input.getAttribute("aria-expanded"));
    assertEquals("", input.getAttribute("aria-controls"));

    box.showSuggestionList();
    Element list = display.getSuggestionMenu().getElement();
    assertEquals("listbox", list.getAttribute("role"));
    assertFalse(list.getId().isEmpty());
    assertEquals("true", input.getAttribute("aria-expanded"));
    assertEquals(list.getId(), input.getAttribute("aria-controls"));
    for (MenuItem item : display.getSuggestionMenu().getItems()) {
      assertEquals("option", item.getElement().getAttribute("role"));
      assertEquals("false", item.getElement().getAttribute("aria-selected"));
      // Chrome only treats the cell as an option inside the listbox if
      // every element in between is explicitly presentational
      for (Element e = item.getElement().getParentElement(); e != list;
          e = e.getParentElement()) {
        assertEquals("presentation", e.getAttribute("role"));
      }
    }
  }

  /**
   * A display that, like many custom ones, doesn't report whether its list
   * is showing.
   */
  private static class PlainSuggestionDisplay extends SuggestionDisplay {
    private int movesDown;

    @Override
    protected Suggestion getCurrentSelection() {
      return null;
    }

    @Override
    protected void hideSuggestions() {
    }

    @Override
    protected void moveSelectionDown() {
      movesDown++;
    }

    @Override
    protected void moveSelectionUp() {
    }

    @Override
    protected void showSuggestions(SuggestBox suggestBox,
        Collection<? extends Suggestion> suggestions,
        boolean isDisplayStringHTML, boolean isAutoSelectEnabled,
        SuggestionCallback callback) {
    }
  }

  public void testCustomDisplayLeavesBoxPlain() {
    SuggestBox box = new SuggestBox(createOracle(), new TextBox(),
        new PlainSuggestionDisplay());
    assertEquals("", box.getElement().getAttribute("role"));
    assertEquals("", box.getElement().getAttribute("aria-expanded"));
  }

  public void testDownArrowMovesCustomDisplayHighlight() {
    PlainSuggestionDisplay display = new PlainSuggestionDisplay();
    SuggestBox box = new SuggestBox(createOracle(), new TextBox(), display);
    RootPanel.get().add(box);
    box.getElement().dispatchEvent(Document.get().createKeyDownEvent(
        false, false, false, false, KeyCodes.KEY_DOWN));
    assertEquals(1, display.movesDown);
  }

  public void testActiveDescendantFollowsHighlight() {
    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = createDefaultsBox(display);
    Element input = box.getElement();
    box.showSuggestionList();
    MenuBar menu = display.getSuggestionMenu();
    Element first = menu.getItems().get(0).getElement();
    Element second = menu.getItems().get(1).getElement();
    assertEquals("", input.getAttribute("aria-activedescendant"));

    display.moveSelectionDown();
    assertEquals(first.getId(), input.getAttribute("aria-activedescendant"));
    assertEquals("true", first.getAttribute("aria-selected"));
    // Only the box, which has focus, names the highlighted item
    assertEquals("", menu.getElement().getAttribute("aria-activedescendant"));

    display.moveSelectionDown();
    assertEquals(second.getId(), input.getAttribute("aria-activedescendant"));
    assertEquals("false", first.getAttribute("aria-selected"));
    assertEquals("true", second.getAttribute("aria-selected"));

    // Hovering moves the highlight that Enter accepts, so it moves this too
    menu.itemOver(menu.getItems().get(0), true);
    assertEquals(first.getId(), input.getAttribute("aria-activedescendant"));

    // Leaving the item clears the highlight
    menu.itemOver(null, false);
    assertEquals("", input.getAttribute("aria-activedescendant"));
  }

  public void testAutoSelectSetsActiveDescendant() {
    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = createDefaultsBox(display);
    box.setAutoSelectEnabled(true);
    box.showSuggestionList();
    Element first = display.getSuggestionMenu().getItems().get(0).getElement();
    assertEquals(first.getId(),
        box.getElement().getAttribute("aria-activedescendant"));
  }

  public void testClosingClearsComboboxState() {
    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = createDefaultsBox(display);
    Element input = box.getElement();

    box.showSuggestionList();
    display.moveSelectionDown();
    display.hideSuggestions();
    assertComboboxClosed(input);

    // An autoHide click outside closes the popup without hideSuggestions()
    box.showSuggestionList();
    display.moveSelectionDown();
    display.getPopupPanel().hide(true);
    assertComboboxClosed(input);

    // Selecting a suggestion closes the list
    box.showSuggestionList();
    display.moveSelectionDown();
    display.getSuggestionMenu().getItems().get(0).getScheduledCommand()
        .execute();
    assertComboboxClosed(input);
  }

  public void testSharedDisplayMovesStateToNewBox() {
    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
    oracle.setDefaultSuggestionsFromText(Arrays.asList("A", "B"));
    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box1 = new SuggestBox(oracle, new TextBox(), display);
    SuggestBox box2 = new SuggestBox(oracle, new TextBox(), display);
    RootPanel.get().add(box1);
    RootPanel.get().add(box2);

    box1.showSuggestionList();
    display.moveSelectionDown();
    box2.showSuggestionList();
    assertComboboxClosed(box1.getElement());
    assertEquals("true", box2.getElement().getAttribute("aria-expanded"));
  }

  public void testListNamedAfterBox() {
    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = createDefaultsBox(display);
    Element input = box.getElement();
    Element list = display.getSuggestionMenu().getElement();

    input.setAttribute("aria-label", "Packages");
    box.showSuggestionList();
    assertEquals("Packages", list.getAttribute("aria-label"));
    assertEquals("", list.getAttribute("aria-labelledby"));

    // aria-labelledby wins over aria-label, as it does for the box
    display.hideSuggestions();
    input.setAttribute("aria-labelledby", "someLabel");
    box.showSuggestionList();
    assertEquals("someLabel", list.getAttribute("aria-labelledby"));
    assertEquals("", list.getAttribute("aria-label"));

    // The box's own id would name the list after the typed text
    display.hideSuggestions();
    input.setId("suggestBoxInput");
    input.setAttribute("aria-labelledby", "someLabel suggestBoxInput");
    box.showSuggestionList();
    assertEquals("someLabel", list.getAttribute("aria-labelledby"));

    display.hideSuggestions();
    input.setAttribute("aria-labelledby", "suggestBoxInput");
    box.showSuggestionList();
    assertEquals("", list.getAttribute("aria-labelledby"));
    assertEquals("Packages", list.getAttribute("aria-label"));
  }

  public void testListNamedAfterLabelElement() {
    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = createDefaultsBox(display);
    Element input = box.getElement();
    input.setId("suggestBoxInput");
    Element label = Document.get().createLabelElement();
    label.setAttribute("for", "suggestBoxInput");
    label.setInnerText("Packages");
    Document.get().getBody().appendChild(label);
    try {
      box.showSuggestionList();
      assertFalse(label.getId().isEmpty());
      assertEquals(label.getId(), display.getSuggestionMenu().getElement()
          .getAttribute("aria-labelledby"));
    } finally {
      label.removeFromParent();
    }
  }

  public void testShowFirst() {
    SuggestBox box = createSuggestBox();
    assertTrue(box.isAutoSelectEnabled());
    SuggestBox box2 = createSuggestBox();
    assertTrue(box2.isAutoSelectEnabled());
    box.setAutoSelectEnabled(false);
    assertFalse(box.isAutoSelectEnabled());
    box.setText("t");
    box.showSuggestionList();
    // Todo(ecc) once event triggering is enabled, submit a return key to the
    // text box and ensure that we see the correct behavior.
  }

  public void testWrapUsingStaticWrapMethod() {
    Element wrapper = Document.get().createTextInputElement();
    RootPanel.get().getElement().appendChild(wrapper);

    // Use direct wrap method from suggest box.
    SuggestBox box = SuggestBox.wrap(createOracle(), wrapper);
    assertTrue(box.isAttached());
    assertTrue(box.getWidget().getParent() == box);
  }

  public void testWrapUsingComposite() {
    // Ensure we can use this with normal composites
    Element wrapper = Document.get().createTextInputElement();
    RootPanel.get().getElement().appendChild(wrapper);
    TextBox b = TextBox.wrap(wrapper);
    SuggestBox box = new SuggestBox(createOracle(), b);
    assertTrue(b.getParent() == box);
  }

  public void testEscapeClosesList() {
    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = createDefaultsBox(display);
    final boolean[] prevented = {false};
    // On the text box, so that it runs after SuggestBox's own handler there
    box.getValueBox().addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
        prevented[0] = isDefaultPrevented(event.getNativeEvent());
      }
    });

    box.showSuggestionList();
    display.moveSelectionDown();
    box.getElement().dispatchEvent(createEscapeKeyDown());
    assertFalse(display.isSuggestionListShowing());
    assertComboboxClosed(box.getElement());
    assertTrue(prevented[0]);

    // With the list closed, Escape is left for others, such as a dialog
    box.getElement().dispatchEvent(createEscapeKeyDown());
    assertFalse(prevented[0]);
  }

  public void testEscapeDropsPendingSuggestions() {
    TestOracle oracle = new TestOracle();
    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = new SuggestBox(oracle, new TextBox(), display);
    RootPanel.get().add(box);
    Collection<MultiWordSuggestion> before = new ArrayList<MultiWordSuggestion>();
    before.add(new MultiWordSuggestion("one", "one"));
    Collection<MultiWordSuggestion> after = new ArrayList<MultiWordSuggestion>();
    after.add(new MultiWordSuggestion("two", "two"));

    box.setText("o");
    box.showSuggestionList();
    SuggestOracle.Request requestBefore = oracle.request;
    SuggestOracle.Callback callbackBefore = oracle.callback;
    box.getElement().dispatchEvent(createEscapeKeyDown());
    callbackBefore.onSuggestionsReady(requestBefore, new Response(before));
    assertFalse(display.isSuggestionListShowing());

    // A request made after Escape shows its response, and one made before
    // still can't replace it
    box.showSuggestionList();
    oracle.callback.onSuggestionsReady(oracle.request, new Response(after));
    assertTrue(display.isSuggestionListShowing());
    callbackBefore.onSuggestionsReady(requestBefore, new Response(before));
    assertEquals("two", display.getSuggestion(0).getReplacementString());
  }

  public void testDownArrowOpensClosedList() {
    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = createDefaultsBox(display);
    box.showSuggestionList();
    box.getElement().dispatchEvent(createEscapeKeyDown());
    assertFalse(display.isSuggestionListShowing());

    box.getElement().dispatchEvent(Document.get().createKeyDownEvent(
        false, false, false, false, KeyCodes.KEY_DOWN));
    assertTrue(display.isSuggestionListShowing());
    // Opening the list doesn't also move the highlight
    assertNull(display.getCurrentSelection());
  }

  public void testDownArrowWaitsForPendingSuggestions() {
    TestOracle oracle = new TestOracle();
    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = createPendingBox(oracle, display);
    SuggestOracle.Request pending = oracle.request;

    box.getElement().dispatchEvent(createDownKeyDown());
    assertSame("Down shouldn't repeat a request still waiting on the oracle",
        pending, oracle.request);
    respond(oracle, "one");
    assertTrue(display.isSuggestionListShowing());
  }

  public void testDownArrowOpeningListPreventsDefault() {
    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = createDefaultsBox(display);
    final boolean[] prevented = {false};
    box.getValueBox().addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
        prevented[0] = isDefaultPrevented(event.getNativeEvent());
      }
    });

    // The oracle answers at once, so the list is open by the time the
    // key's default action would run
    box.getElement().dispatchEvent(createDownKeyDown());
    assertTrue(display.isSuggestionListShowing());
    assertTrue(prevented[0]);
  }

  private static NativeEvent createDownKeyDown() {
    return Document.get().createKeyDownEvent(false, false, false, false,
        KeyCodes.KEY_DOWN);
  }

  public void testTabDropsPendingSuggestions() {
    TestOracle oracle = new TestOracle();
    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = createPendingBox(oracle, display);
    box.getElement().dispatchEvent(Document.get().createKeyDownEvent(
        false, false, false, false, KeyCodes.KEY_TAB));
    respond(oracle, "one");
    assertFalse(display.isSuggestionListShowing());
  }

  public void testBlurDropsPendingSuggestions() {
    TestOracle oracle = new TestOracle();
    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = createPendingBox(oracle, display);
    box.getElement().dispatchEvent(Document.get().createBlurEvent());
    respond(oracle, "one");
    assertFalse(display.isSuggestionListShowing());
  }

  public void testOlderResponseCantReplaceNewer() {
    TestOracle oracle = new TestOracle();
    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = createPendingBox(oracle, display);
    SuggestOracle.Request olderRequest = oracle.request;
    SuggestOracle.Callback olderCallback = oracle.callback;

    box.setText("on");
    box.showSuggestionList();
    respond(oracle, "one");
    olderCallback.onSuggestionsReady(olderRequest, createResponse("other"));
    assertEquals("one", display.getSuggestion(0).getReplacementString());
  }

  /**
   * An attached box with a request for "o" still waiting on the oracle.
   */
  private static SuggestBox createPendingBox(TestOracle oracle,
      TestSuggestionDisplay display) {
    SuggestBox box = new SuggestBox(oracle, new TextBox(), display);
    RootPanel.get().add(box);
    box.setText("o");
    box.showSuggestionList();
    return box;
  }

  private static void respond(TestOracle oracle, String suggestion) {
    oracle.callback.onSuggestionsReady(oracle.request,
        createResponse(suggestion));
  }

  private static Response createResponse(String suggestion) {
    Collection<MultiWordSuggestion> suggestions = new ArrayList<MultiWordSuggestion>();
    suggestions.add(new MultiWordSuggestion(suggestion, suggestion));
    return new Response(suggestions);
  }

  private static NativeEvent createEscapeKeyDown() {
    return Document.get().createKeyDownEvent(false, false, false, false,
        KeyCodes.KEY_ESCAPE);
  }

  private static native boolean isDefaultPrevented(NativeEvent event) /*-{
    return event.defaultPrevented;
  }-*/;

  /**
   * See https://code.google.com/p/google-web-toolkit/issues/detail?id=3533
   */
  public void testKeyDownEvent() {
    final int[] eventFireCount = {0};
    SuggestBox box = new SuggestBox();
    RootPanel.get().add(box);
    box.addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
        eventFireCount[0]++;
      }
    });
    NativeEvent e = Document.get().createKeyDownEvent(false, false, false, false, 'g');
    box.getElement().dispatchEvent(e);
    assertEquals(1, eventFireCount[0]);
  }

  /**
   * See https://code.google.com/p/google-web-toolkit/issues/detail?id=3533
   */
  public void testKeyUpEvent() {
    final int[] eventFireCount = {0};
    SuggestBox box = new SuggestBox();
    RootPanel.get().add(box);
    box.addKeyUpHandler(new KeyUpHandler() {
      @Override
      public void onKeyUp(KeyUpEvent event) {
        eventFireCount[0]++;
      }
    });
    NativeEvent e = Document.get().createKeyUpEvent(false, false, false, false, 'g');
    box.getElement().dispatchEvent(e);
    assertEquals(1, eventFireCount[0]);
  }

  protected SuggestBox createSuggestBox() {
    MultiWordSuggestOracle oracle = createOracle();
    return new SuggestBox(oracle, new TextBox(), new TestSuggestionDisplay());
  }

  /**
   * An attached box whose list shows "A" and "B" when empty, with nothing
   * highlighted until the arrow keys move.
   */
  private SuggestBox createDefaultsBox(TestSuggestionDisplay display) {
    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
    oracle.setDefaultSuggestionsFromText(Arrays.asList("A", "B"));
    SuggestBox box = new SuggestBox(oracle, new TextBox(), display);
    box.setAutoSelectEnabled(false);
    RootPanel.get().add(box);
    return box;
  }

  private static void assertComboboxClosed(Element input) {
    assertEquals("false", input.getAttribute("aria-expanded"));
    assertEquals("", input.getAttribute("aria-controls"));
    assertEquals("", input.getAttribute("aria-activedescendant"));
  }

  private MultiWordSuggestOracle createOracle() {
    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
    oracle.add("test");
    oracle.add("test1");
    oracle.add("test2");
    oracle.add("test3");
    oracle.add("test4");
    oracle.add("john");
    return oracle;
  }
}
