package org.unicode.cldr.draft;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.xerces.dom.NotationImpl;
import org.unicode.cldr.draft.CharacterListCompressor.Interval;
import org.unicode.cldr.draft.PickerData;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRTool;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.VersionInfo;

@CLDRTool(alias="picker", description="DEMO Character Picker app based on CLDR data")
public class PickerApp implements ListSelectionListener {
	private final Font mainFont;
	private final Font pickerFont;
	private final Font biggerFont;
	private final class CharPickerCellRenderer extends JLabel implements
			ListCellRenderer<String>  {
		/**
		 *
		 */
		private static final long serialVersionUID = 869587839960963873L;

		CharPickerCellRenderer() {
			super();
			setOpaque(true);
	        setHorizontalAlignment(CENTER);
	        setVerticalAlignment(CENTER);
			setFont(pickerFont);
		}

		@Override
		public Component getListCellRendererComponent(
				JList<? extends String> list, String value, int index,
				boolean isSelected, boolean cellHasFocus) {
			setText(value);
			setBackground(isSelected?Color.YELLOW:frame.getBackground());
			return this;
		}
	}

	final private JList<String> catList;
	final private JList<String> grpList;
	final private JList<String> charList;
	final private JPanel   label;
	final private JFrame frame;

	public static void main(String args[]) {
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				new PickerApp();
			}
		});
	}

	public PickerApp() {

		mainFont = Font.decode("dialog-PLAIN-18");
		pickerFont = mainFont.deriveFont(24);
		biggerFont = mainFont.deriveFont(36);

		frame = new JFrame("CLDR "+CLDRFile.GEN_VERSION+" CharPicker http://cldr.unicode.org, ICU:"+VersionInfo.ICU_VERSION);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(512, 342);

		//				JLabel l = new JLabel("s+rl");
		//				j.getContentPane().add(l);
		frame.setLayout(new GridLayout(3, 1));
		ListModel<String> model = new ListModel<String>() {
	        final List<String> categories = PickerData.CATEGORIES;

            @Override
            public int getSize() {
                // TODO Auto-generated method stub
                return categories.size();
            }

            @Override
            public String getElementAt(int index) {
                // TODO Auto-generated method stub
                return categories.get(index);
            }

            @Override
            public void addListDataListener(ListDataListener l) {
                // never changes
                //throw new InternalError("foo");
            }

            @Override
            public void removeListDataListener(ListDataListener l) {
               // throw new InternalError("foo");
            }

		};
		catList = new JList<String>(model);
		catList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		catList.addListSelectionListener(this);

		grpList = new JList<String>();
		grpList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		grpList.addListSelectionListener(this);

		charList = new JList<String>();
		charList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		charList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		charList.setCellRenderer(new CharPickerCellRenderer());
		charList.addListSelectionListener(this);

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		JScrollPane catScroll = new JScrollPane(catList);
		split.add(catScroll);
		JScrollPane grpScroll = new JScrollPane(grpList);
		split.add(grpScroll);
		frame.add(split);
		JScrollPane charScroll = new JScrollPane(charList);
		frame.add(charScroll);
		label = new JPanel();
		label.setLayout(new GridLayout(5,1));

		label.add(new JLabel("Basic char picker. Pick some categories to get started."));

		frame.add(label);
		frame.setVisible(true);
		frame.pack();
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		//if(!e.getValueIsAdjusting()) return;
		//System.err.println(e.toString());
		int c = e.getFirstIndex();
		if(e.getSource() == catList) {
			//System.err.println("catlist click " + c);
			grpList.setListData(PickerData.getSubCategories(c));
			frame.pack();
		} else if(e.getSource() == grpList) {
			//System.err.println("grplist click" + c);
			List<Interval> chars = PickerData.getStringArray(catList.getSelectedIndex(), grpList.getSelectedIndex());
			Vector<String> stringVector  =new Vector<String>();
			for(Interval interval : chars) {
				for(int i=interval.first();i<=interval.last();i++) {
					stringVector.add(UCharacter.toString(i));
				}
			}
			charList.setListData(stringVector.toArray(new String[stringVector.size()]));
			frame.pack();
		} else if(e.getSource() == charList) {
			final String str = charList.getSelectedValue();
			label.removeAll();
			JLabel charStr = new JLabel(str);
			charStr.setFont(biggerFont);
			label.add(charStr);
			{
				StringBuilder sb = new StringBuilder();
				final int cp = UCharacter.codePointAt(str, 0);
				sb.append("U+");
				String hstr = Integer.toHexString(cp);
				for(int i=2;i<=4;i++) {
					if(hstr.length()<i) {
						sb.append('0');
					}
				}
				sb.append(hstr);
				sb.append(' ');
				sb.append(UCharacter.getName(cp));
				sb.append('\n');
				label.add(new JLabel(sb.toString()));

				JButton copyButton = new JButton("Copy");
				copyButton.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent e) {
						Toolkit.getDefaultToolkit().getSystemClipboard()
							.setContents(new StringSelection(str),null);
					}});
				label.add(copyButton);

				label.setVisible(true);
				frame.pack();
			}
		}
	}
}
