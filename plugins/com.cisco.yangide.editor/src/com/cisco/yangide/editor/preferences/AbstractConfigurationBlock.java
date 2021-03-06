/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.cisco.yangide.editor.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;

import com.cisco.yangide.editor.dialogs.StatusInfo;
import com.cisco.yangide.editor.dialogs.StatusUtil;
import com.cisco.yangide.ui.preferences.OverlayPreferenceStore;

/**
 * Configures YANG Editor typing preferences.
 *
 * @author Alexey Kholupko
 */
abstract class AbstractConfigurationBlock implements IPreferenceConfigurationBlock {

    protected final class SectionManager {
        /** The preference setting for keeping no section open. */
        private static final String __NONE = "__none"; //$NON-NLS-1$
        private Set<ExpandableComposite> fSections = new HashSet<ExpandableComposite>();
        private boolean fIsBeingManaged = false;
        private ExpansionAdapter fListener = new ExpansionAdapter() {
            @Override
            public void expansionStateChanged(ExpansionEvent e) {
                ExpandableComposite source = (ExpandableComposite) e.getSource();
                updateSectionStyle(source);
                if (fIsBeingManaged)
                    return;
                if (e.getState()) {
                    try {
                        fIsBeingManaged = true;
                        for (Iterator<ExpandableComposite> iter = fSections.iterator(); iter.hasNext();) {
                            ExpandableComposite composite = iter.next();
                            if (composite != source)
                                composite.setExpanded(false);
                        }
                    } finally {
                        fIsBeingManaged = false;
                    }
                    if (fLastOpenKey != null && fDialogSettingsStore != null)
                        fDialogSettingsStore.setValue(fLastOpenKey, source.getText());
                } else {
                    if (!fIsBeingManaged && fLastOpenKey != null && fDialogSettingsStore != null)
                        fDialogSettingsStore.setValue(fLastOpenKey, __NONE);
                }
                ExpandableComposite exComp = getParentExpandableComposite(source);
                if (exComp != null)
                    exComp.layout(true, true);
                ScrolledPageContent parentScrolledComposite = getParentScrolledComposite(source);
                if (parentScrolledComposite != null) {
                    parentScrolledComposite.reflow(true);
                }
            }
        };
        private Composite fBody;
        private final String fLastOpenKey;
        private final IPreferenceStore fDialogSettingsStore;
        private ExpandableComposite fFirstChild = null;

        /**
         * Creates a new section manager.
         */
        public SectionManager() {
            this(null, null);
        }

        /**
         * Creates a new section manager.
         * 
         * @param dialogSettingsStore the dialog store
         * @param lastOpenKey the preference key
         */
        public SectionManager(IPreferenceStore dialogSettingsStore, String lastOpenKey) {
            fDialogSettingsStore = dialogSettingsStore;
            fLastOpenKey = lastOpenKey;
        }

        private void manage(ExpandableComposite section) {
            if (section == null)
                throw new NullPointerException();
            if (fSections.add(section))
                section.addExpansionListener(fListener);
            makeScrollableCompositeAware(section);
        }

        public Composite createSectionComposite(Composite parent) {
            Assert.isTrue(fBody == null);
            boolean isNested = isNestedInScrolledComposite(parent);
            Composite composite;
            if (isNested) {
                composite = new Composite(parent, SWT.NONE);
                fBody = composite;
            } else {
                composite = new ScrolledPageContent(parent);
                fBody = ((ScrolledPageContent) composite).getBody();
            }

            fBody.setLayout(new GridLayout());

            return composite;
        }

        public Composite createSection(String label) {
            Assert.isNotNull(fBody);
            final ExpandableComposite excomposite = new ExpandableComposite(fBody, SWT.NONE,
                    ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT | ExpandableComposite.COMPACT);
            if (fFirstChild == null)
                fFirstChild = excomposite;
            excomposite.setText(label);
            String last = null;
            if (fLastOpenKey != null && fDialogSettingsStore != null)
                last = fDialogSettingsStore.getString(fLastOpenKey);

            if (fFirstChild == excomposite && !__NONE.equals(last) || label.equals(last)) {
                excomposite.setExpanded(true);
                if (fFirstChild != excomposite)
                    fFirstChild.setExpanded(false);
            } else {
                excomposite.setExpanded(false);
            }
            excomposite.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));

            updateSectionStyle(excomposite);
            manage(excomposite);

            Composite contents = new Composite(excomposite, SWT.NONE);
            excomposite.setClient(contents);

            return contents;
        }
    }

    public class ScrolledPageContent extends SharedScrolledComposite {

        private FormToolkit fToolkit;

        public ScrolledPageContent(Composite parent) {
            this(parent, SWT.V_SCROLL | SWT.H_SCROLL);
        }

        public ScrolledPageContent(Composite parent, int style) {
            super(parent, style);

            setFont(parent.getFont());

            FormColors colors = new FormColors(Display.getCurrent());
            colors.setBackground(null);
            colors.setForeground(null);
            fToolkit = new FormToolkit(colors);

            setExpandHorizontal(true);
            setExpandVertical(true);

            Composite body = new Composite(this, SWT.NONE);
            body.setFont(parent.getFont());
            setContent(body);
        }

        public void adaptChild(Control childControl) {
            fToolkit.adapt(childControl, true, true);
        }

        public Composite getBody() {
            return (Composite) getContent();
        }

    }

    protected static final int INDENT = 20;
    private OverlayPreferenceStore fStore;

    private Map<Button, String> fCheckBoxes = new HashMap<Button, String>();
    private SelectionListener fCheckBoxListener = new SelectionListener() {
        public void widgetDefaultSelected(SelectionEvent e) {
        }

        public void widgetSelected(SelectionEvent e) {
            Button button = (Button) e.widget;
            fStore.setValue(fCheckBoxes.get(button), button.getSelection());
        }
    };

    private Map<Text, String> fTextFields = new HashMap<Text, String>();
    private ModifyListener fTextFieldListener = new ModifyListener() {
        public void modifyText(ModifyEvent e) {
            Text text = (Text) e.widget;
            fStore.setValue(fTextFields.get(text), text.getText());
        }
    };

    private ArrayList<Text> fNumberFields = new ArrayList<Text>();
    private ModifyListener fNumberFieldListener = new ModifyListener() {
        public void modifyText(ModifyEvent e) {
            numberFieldChanged((Text) e.widget);
        }
    };

    /**
     * List of master/slave listeners when there's a dependency.
     *
     * @see #createDependency(Button, Control)
     */
    private ArrayList<SelectionListener> fMasterSlaveListeners = new ArrayList<SelectionListener>();

    private StatusInfo fStatus;
    private final PreferencePage fMainPage;

    public AbstractConfigurationBlock(OverlayPreferenceStore store) {
        Assert.isNotNull(store);
        fStore = store;
        fMainPage = null;
    }

    public AbstractConfigurationBlock(OverlayPreferenceStore store, PreferencePage mainPreferencePage) {
        Assert.isNotNull(store);
        Assert.isNotNull(mainPreferencePage);
        fStore = store;
        fMainPage = mainPreferencePage;
    }

    protected final ScrolledPageContent getParentScrolledComposite(Control control) {
        Control parent = control.getParent();
        while (!(parent instanceof ScrolledPageContent) && parent != null) {
            parent = parent.getParent();
        }
        if (parent instanceof ScrolledPageContent) {
            return (ScrolledPageContent) parent;
        }
        return null;
    }

    private final ExpandableComposite getParentExpandableComposite(Control control) {
        Control parent = control.getParent();
        while (!(parent instanceof ExpandableComposite) && parent != null) {
            parent = parent.getParent();
        }
        if (parent instanceof ExpandableComposite) {
            return (ExpandableComposite) parent;
        }
        return null;
    }

    protected void updateSectionStyle(ExpandableComposite excomposite) {
        excomposite.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
    }

    protected void makeScrollableCompositeAware(Control control) {
        ScrolledPageContent parentScrolledComposite = getParentScrolledComposite(control);
        if (parentScrolledComposite != null) {
            parentScrolledComposite.adaptChild(control);
        }
    }

    private boolean isNestedInScrolledComposite(Composite parent) {
        return getParentScrolledComposite(parent) != null;
    }

    protected Button addCheckBox(Composite parent, String label, String key, int indentation) {
        Button checkBox = new Button(parent, SWT.CHECK);
        checkBox.setText(label);

        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalIndent = indentation;
        gd.horizontalSpan = 2;
        checkBox.setLayoutData(gd);
        checkBox.addSelectionListener(fCheckBoxListener);
        makeScrollableCompositeAware(checkBox);

        fCheckBoxes.put(checkBox, key);

        return checkBox;
    }

    /**
     * Returns an array of size 2: - first element is of type <code>Label</code> - second element is
     * of type <code>Text</code> Use <code>getLabelControl</code> and <code>getTextControl</code> to
     * get the 2 controls.
     */
    protected Control[] addLabelledTextField(Composite composite, String label, String key, int textLimit,
            int indentation, boolean isNumber) {

        PixelConverter pixelConverter = new PixelConverter(composite);

        Label labelControl = new Label(composite, SWT.NONE);
        labelControl.setText(label);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalIndent = indentation;
        labelControl.setLayoutData(gd);

        Text textControl = new Text(composite, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = pixelConverter.convertWidthInCharsToPixels(textLimit + 1);
        textControl.setLayoutData(gd);
        textControl.setTextLimit(textLimit);
        fTextFields.put(textControl, key);
        if (isNumber) {
            fNumberFields.add(textControl);
            textControl.addModifyListener(fNumberFieldListener);
        } else {
            textControl.addModifyListener(fTextFieldListener);
        }

        return new Control[] { labelControl, textControl };
    }

    protected void createDependency(final Button master, final Control slave) {
        Assert.isNotNull(slave);
        indent(slave);
        SelectionListener listener = new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                boolean state = master.getSelection();
                slave.setEnabled(state);
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        };
        master.addSelectionListener(listener);
        fMasterSlaveListeners.add(listener);
    }

    protected static void indent(Control control) {
        ((GridData) control.getLayoutData()).horizontalIndent += 20;// LayoutUtil.getIndent();
    }

    public void initialize() {
        initializeFields();
    }

    private void initializeFields() {

        Iterator<Button> iter = fCheckBoxes.keySet().iterator();
        while (iter.hasNext()) {
            Button b = iter.next();
            String key = fCheckBoxes.get(b);
            b.setSelection(fStore.getBoolean(key));
        }

        Iterator<Text> iter2 = fTextFields.keySet().iterator();
        while (iter2.hasNext()) {
            Text t = iter2.next();
            String key = fTextFields.get(t);
            t.setText(fStore.getString(key));
        }

        // Update slaves
        Iterator<SelectionListener> iter3 = fMasterSlaveListeners.iterator();
        while (iter3.hasNext()) {
            SelectionListener listener = iter3.next();
            listener.widgetSelected(null);
        }

        updateStatus(new StatusInfo());
    }

    public void performOk() {
    }

    public void performDefaults() {
        initializeFields();
    }

    IStatus getStatus() {
        if (fStatus == null)
            fStatus = new StatusInfo();
        return fStatus;
    }

    /*
     * @see com.cisco.yangide.editor.preferences.IPreferenceConfigurationBlock#dispose()
     */
    public void dispose() {
    }

    private void numberFieldChanged(Text textControl) {
        String number = textControl.getText();
        IStatus status = validatePositiveNumber(number);
        if (!status.matches(IStatus.ERROR))
            fStore.setValue(fTextFields.get(textControl), number);
        updateStatus(status);
    }

    private IStatus validatePositiveNumber(String number) {
        StatusInfo status = new StatusInfo();
        if (number.length() == 0) {
            status.setError(YangPreferencesMessages.YANGEditorPreferencePage_empty_input);
        } else {
            try {
                int value = Integer.parseInt(number);
                if (value < 0)
                    status.setError(String.format(YangPreferencesMessages.YANGEditorPreferencePage_invalid_input,
                            number));
            } catch (NumberFormatException e) {
                status.setError(String.format(YangPreferencesMessages.YANGEditorPreferencePage_invalid_input, number));
            }
        }
        return status;
    }

    protected void updateStatus(IStatus status) {
        if (fMainPage == null)
            return;
        fMainPage.setValid(status.isOK());
        StatusUtil.applyToStatusLine(fMainPage, status);
    }

    protected final OverlayPreferenceStore getPreferenceStore() {
        return fStore;
    }

    protected Composite createSubsection(Composite parent, SectionManager manager, String label) {
        if (manager != null) {
            return manager.createSection(label);
        } else {
            Group group = new Group(parent, SWT.SHADOW_NONE);
            group.setText(label);
            GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
            group.setLayoutData(data);
            return group;
        }
    }
}
