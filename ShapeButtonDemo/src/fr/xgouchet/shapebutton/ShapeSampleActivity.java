package fr.xgouchet.shapebutton;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import fr.xgouchet.shapebutton.widget.ShapeButton;
import fr.xgouchet.shapebutton.widget.ShapeButton.OnClickListener;

public class ShapeSampleActivity extends Activity implements OnClickListener {

	/**
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	protected void onCreate(Bundle savedInstanceState) {
		LayoutInflater.from(this).setFactory(new ShapeButtonViewFactory());

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_demo);

		mShapeButton = ((ShapeButton) findViewById(R.id.shapeButton));
		mShapeButton.setOnClickListener(this);

		int xml = getIntent().getIntExtra("xml", 0);
		if (xml > 0) {
			mShapeButton.setShapesXml(xml);
		}
	}

	public void onClick(View v, String id) {
		if (id != null) {
			Toast.makeText(this, id, Toast.LENGTH_SHORT).show();
		}
	}

	private ShapeButton mShapeButton;

}
