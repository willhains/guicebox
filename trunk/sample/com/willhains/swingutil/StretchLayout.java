package com.willhains.swingutil;

import java.awt.*;
import java.util.*;

/**
 * Greatly simplified approach to layout of business forms. This layout manager is designed not for visual GUI building
 * tools, but for coding form layout by hand. It allows forms to be structured similarly to GridBagLayout and HTML
 * tables, as a grid of rows and columns, with some cells spanning multiple rows and/or columns. EasyLayout simplifies
 * this approach by defining the overall layout of the screen before adding any components, and by reducing the
 * "weights" of cells in GridBagLayout to allow one row and one column to be "stretchy".
 * 
 * @author willhains
 */
public class StretchLayout extends GridBagLayout
{
	private static final long serialVersionUID = 4304370210422763460L;
	
	private final Map<String, GridBagConstraints> _constraints = new HashMap<String, GridBagConstraints>();
	
	public StretchLayout(int stretchyRow, int stretchyCol, String[][] layout)
	{
		this(stretchyRow, stretchyCol, 5, layout);
	}
	
	public StretchLayout(int stretchyRow, int stretchyCol, int padding, String[][] layout)
	{
		this(stretchyRow, stretchyCol, padding, padding, layout);
	}
	
	public StretchLayout(int stretchyRow, int stretchyCol, int internalPadding, int borderPadding, String[][] layout)
	{
		for(int r = 0; r < layout.length; r++)
		{
			for(int c = 0; c < layout[r].length; c++)
			{
				final String region = layout[r][c];
				GridBagConstraints gc = _constraints.get(region);
				if(gc == null)
				{
					_constraints.put(region, gc = new GridBagConstraints());
					gc.gridx = c;
					gc.gridy = r;
					gc.fill = GridBagConstraints.BOTH;
				}
				if(r == stretchyRow) gc.weighty = 1;
				if(c == stretchyCol) gc.weightx = 1;
				gc.insets = new Insets( //
				gc.gridy == 0 ? borderPadding : internalPadding, gc.gridx == 0 ? borderPadding : internalPadding, r == layout.length - 1
					? borderPadding
					: 0, c == layout[r].length - 1 ? borderPadding : 0);
				gc.gridheight = r - gc.gridy + 1;
				gc.gridwidth = c - gc.gridx + 1;
				
				// Leave this here - it's useful for debugging
				// log.debug(region + "= [" + gc.gridx + "," + gc.gridy + "] (" + gc.gridwidth + "x"
				// + gc.gridheight + ") " + gc.weightx + " " + gc.weighty + " {" + gc.insets.top + "|"
				// + gc.insets.left + "|" + gc.insets.bottom + "|" + gc.insets.right + "}");
			}
		}
	}
	
	@Override public void addLayoutComponent(Component c, Object constraints)
	{
		super.addLayoutComponent(c, _constraints.get(constraints));
	}
}
