package fr.xgouchet.shapebutton;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater.Factory;
import android.view.View;
import fr.xgouchet.shapebutton.widget.ShapeButton;

public class ShapeButtonViewFactory implements Factory {

	public View onCreateView(final String name, final Context context,
			final AttributeSet attrs) {

		if (TextUtils.equals(name, "ShapeButton")) {
			return new ShapeButton(context, attrs);
		} else if (TextUtils.equals(name,
				"fr.xgouchet.shapebutton.widget.ShapeButton")) {
			return new ShapeButton(context, attrs);
		} else {
			return null;
		}
	}
}