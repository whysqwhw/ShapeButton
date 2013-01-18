package fr.xgouchet.shapebutton;

import android.app.ListActivity;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

public class MainActivity extends ListActivity implements OnItemClickListener {

	/**
	 * @see android.app.Activity#onResume()
	 */
	protected void onResume() {
		super.onResume();

		String[] data = getResources().getStringArray(R.array.sample_names);
		mXMLs = getResources().getStringArray(R.array.sample_xml);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, data);
		setListAdapter(adapter);
		getListView().setOnItemClickListener(this);
	}

	/**
	 * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView,
	 *      android.view.View, int, long)
	 */
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {

		int xmlId = getResources().getIdentifier(mXMLs[position], "xml",
				getPackageName());

		Intent intent = new Intent(this, ShapeSampleActivity.class);
		intent.putExtra("xml", xmlId);

		startActivity(intent);
	}

	private String[] mXMLs;
}
