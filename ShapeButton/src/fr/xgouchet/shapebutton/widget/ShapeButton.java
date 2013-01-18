package fr.xgouchet.shapebutton.widget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.FillType;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import fr.xgouchet.shapebutton.R;

public class ShapeButton extends View {

	/**
	 * Interface definition for a callback to be invoked when a view is clicked.
	 */
	public interface OnClickListener {
		/**
		 * Called when a view has been clicked.
		 * 
		 * @param v
		 *            The view that was clicked.
		 * @param id
		 *            the id of the shape that was clicked
		 */
		void onClick(View v, String id);
	}

	public enum ShapeType {
		rect, oval, triangle, arc
	}

	/**
	 * Defines the shape to be used in a {@link ShapeButton}, as well as its
	 * fundamental properties (color, ...)
	 */
	public class ShapeElement implements Comparable<ShapeElement> {

		public static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

		/**
		 */
		public ShapeElement(final ShapeType shapeType) {
			// members
			mShape = shapeType;
			mRect = new Rect();
			mRectF = new RectF();
			mPath = new Path();
			mPosition = new Point();
			mEnabled = true;

			updatePath();

			// set default color and paint settings
			mPath.setFillType(FillType.WINDING);
			mPaint = new Paint();
			mPaint.setAntiAlias(true);
			mPaint.setStyle(Paint.Style.FILL);
			mPaint.setMaskFilter(new EmbossMaskFilter(new float[] { 1, 1, 1 },
					0.8f, 3.0f, 1 * mDipToPixel));

			mBase = Color.argb(255, 214, 214, 214);
			mAccent = Color.argb(255, 51, 173, 214);
		}

		/**
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(ShapeElement another) {
			return mZOrder - another.mZOrder;
		}

		/**
		 * Draws the shape on the canvas
		 * 
		 * @param canvas
		 *            the canvas to draw on
		 */
		public void onDraw(final Canvas canvas) {

			// set paint color
			if (!isEnabled()) {
				mPaint.setColor(mBase);
				mPaint.setAlpha(128);
			} else if (isPressed()) {
				mPaint.setColor(mAccent);
			} else {
				mPaint.setColor(mBase);
			}

			// actually draw
			canvas.save();
			canvas.translate(mPosition.x, mPosition.y);
			canvas.rotate(mAngle, mRect.width() / 2, mRect.height() / 2);
			drawShape(canvas);
			canvas.restore();
		}

		/**
		 * @param canvas
		 */
		private void drawShape(final Canvas canvas) {
			switch (mShape) {
			case rect:
				canvas.drawRect(mRect, mPaint);
				break;
			case oval:
				canvas.drawOval(mRectF, mPaint);
				break;
			case triangle:
			case arc:
				canvas.drawPath(mPath, mPaint);
				break;
			}

		}

		private void updatePath() {
			float hw, hh;
			hw = (mRectF.width() / 2.0f);
			hh = (mRectF.height() / 2.0f);

			switch (mShape) {
			case triangle:
				mPath.reset();
				mPath.moveTo(hw, 0.0f);
				mPath.lineTo(mRectF.width(), mRectF.height());
				mPath.lineTo(0, mRectF.height());
				mPath.close();
				break;
			case arc:
				mPath.reset();
				if (mThickness > 0) {
					mPath.addArc(mRectF, mStart, (mEnd - mStart));
					mPath.lineTo(hw + FloatMath.cos(mEnd * DEG_TO_RAD)
							* (hw - mThickness),
							hh + FloatMath.sin(mEnd * DEG_TO_RAD)
									* (hh - mThickness));
					RectF inside = new RectF(mThickness, mThickness,
							mRectF.width() - mThickness, mRectF.height()
									- mThickness);
					mPath.addArc(inside, mEnd, (mStart - mEnd));
					mPath.lineTo(hw + FloatMath.cos(mStart * DEG_TO_RAD) * hw,
							hh + FloatMath.sin(mStart * DEG_TO_RAD) * hh);

				} else {
					mPath.addArc(mRectF, mStart, mEnd - mStart);
					mPath.lineTo(hw, hh);
					mPath.close();
				}

			default:
				break;
			}
		}

		/**
		 * @return the needed width
		 */
		public int getNeededWidth() {
			float halfwidth = (mRect.width() / 2);

			float max = getRealWidth() / 2;

			return (int) (mPosition.x + halfwidth + max + 0.5f);
		}

		/**
		 * @return the needed height
		 */
		public int getNeededHeight() {
			float halfheight = (mRect.height() / 2);

			float max = getRealHeight() / 2.0f;

			return (int) (mPosition.y + halfheight + max + 0.5f);
		}

		/**
		 * @param x
		 * @param y
		 * @return if the touch is inside the current shape
		 */
		public boolean checkTouch(final float x, final float y) {

			if (!mEnabled) {
				return false;
			}

			float x1, y1, cos, sin, cx, cy, x2, y2;

			cx = mPosition.x + (mRect.width() / 2);
			cy = mPosition.y + (mRect.height() / 2);
			cos = FloatMath.cos(mAngle * DEG_TO_RAD);
			sin = FloatMath.sin(mAngle * DEG_TO_RAD);

			x1 = cos * (x - cx) + sin * (y - cy);
			y1 = -sin * (x - cx) + cos * (y - cy);

			boolean res;
			switch (mShape) {
			case rect:
			case triangle: // TODO check triangle touch
				res = (Math.abs(x1) <= (mRectF.width() / 2));
				res &= (Math.abs(y1) <= (mRectF.height() / 2));
				break;
			case oval:
				x2 = x1 / (mRectF.width() / 2);
				y2 = y1 / (mRectF.height() / 2);
				res = ((x2 * x2) + (y2 * y2) < 1);
				break;
			case arc:
				x2 = x1 / (mRectF.width() / 2);
				y2 = y1 / (mRectF.height() / 2);
				res = ((x2 * x2) + (y2 * y2) < 1);

				if (mThickness > 0) {
					x2 = x1 / ((mRectF.width() / 2) - mThickness);
					y2 = y1 / ((mRectF.height() / 2) - mThickness);
					res &= ((x2 * x2) + (y2 * y2) > 1);
				}

				float angle = (float) (Math.atan2(y1, x1) / DEG_TO_RAD);
				res &= isAngleInRange(angle);
				break;
			default:
				res = false;
				break;
			}

			return res;
		}

		private boolean isAngleInRange(float angle) {

			float min, max, value;
			min = Math.min(mStart, mEnd);
			max = Math.max(mStart, mEnd);

			value = angle;
			while (value < min) {
				value += 360;
			}

			return value < max;

		}

		/**
		 * @return the height axis aligned
		 */
		private float getRealHeight() {
			float cos = Math.abs(FloatMath.cos(mAngle * DEG_TO_RAD));
			float sin = Math.abs(FloatMath.sin(mAngle * DEG_TO_RAD));
			return ((sin * mRect.width()) + (cos * mRect.height()));
		}

		/**
		 * @return the width axis aligned
		 */
		private float getRealWidth() {
			float cos = Math.abs(FloatMath.cos(mAngle * DEG_TO_RAD));
			float sin = Math.abs(FloatMath.sin(mAngle * DEG_TO_RAD));
			return ((cos * mRect.width()) + (sin * mRect.height()));
		}

		/**
		 * @return the zOrder
		 */
		public int getZOrder() {
			return mZOrder;
		}

		/**
		 * @return the shape id
		 */
		public String getId() {
			return mId;
		}

		/**
		 * @return the pressed
		 */
		public boolean isPressed() {
			return mPressed;
		}

		/**
		 * @return the enabled
		 */
		public boolean isEnabled() {
			return mEnabled;
		}

		/**
		 * @param id
		 */
		public void setId(String id) {
			mId = id;
		}

		/**
		 * @param angle
		 *            the rotation angle (degrees)
		 */
		public void setAngle(int angle) {
			this.mAngle = angle;
		}

		/**
		 * @param width
		 *            the width of the shape (in pixels)
		 * @param height
		 *            the height of the shape (in pixels)
		 */
		public void setSize(final int width, final int height) {
			mRect.set(0, 0, width, height);
			mRectF.set(0, 0, width, height);
			updatePath();
		}

		/**
		 * @param position
		 *            the position of the shape
		 */
		public void setPosition(final Point pos) {
			mPosition.set(pos.x, pos.y);
		}

		/**
		 * @param x
		 *            the x position of the shape
		 * @param y
		 *            the y position of the shape
		 */
		public void setPosition(final int x, final int y) {
			mPosition.set(x, y);
		}

		/**
		 * @param x
		 *            the x position of the shape
		 * @param y
		 *            the y position of the shape
		 * @param z
		 *            the z order of the shape
		 */
		public void setPosition(final int x, final int y, final int z) {
			mPosition.set(x, y);
			mZOrder = z;
		}

		/**
		 * @param base
		 *            the base color to set
		 */
		public void setBaseColor(final int base) {
			mBase = base;
		}

		/**
		 * @param accent
		 *            the accent color to set
		 */
		public void setAccentColor(final int accent) {
			mAccent = accent;
		}

		/**
		 * @param zOrder
		 *            the zOrder to set
		 */
		public void setZOrder(final int zOrder) {
			mZOrder = zOrder;
		}

		/**
		 * @param pressed
		 *            the pressed to set
		 */
		public void setPressed(final boolean pressed) {
			mPressed = pressed;
		}

		/**
		 * @param enabled
		 *            the enabled to set
		 */
		public void setEnabled(final boolean enabled) {
			mEnabled = enabled;
		}

		/**
		 * Sets a range (angles for arc)
		 * 
		 * @param start
		 * @param end
		 */
		public void setRange(final int start, final int end) {
			mStart = start;
			mEnd = end;
			updatePath();
		}

		/**
		 * @param thickness
		 */
		public void setThickness(int thickness) {
			mThickness = thickness;
			updatePath();
		}

		final private Point mPosition;
		final private Paint mPaint;
		final private Rect mRect;
		final private RectF mRectF;
		final private Path mPath;
		final private ShapeType mShape;

		private String mId;

		private int mBase, mAccent;
		private int mZOrder, mAngle, mStart, mEnd, mThickness;
		private boolean mPressed, mEnabled;
	}

	/**
	 * Simple constructor to use when creating a view from code.
	 * 
	 * @param context
	 *            The Context the view is running in, through which it can
	 *            access the current theme, resources, etc.
	 */
	public ShapeButton(Context context) {
		super(context);
		initShapeButton();
	}

	/**
	 * Constructor that is called when inflating a view from XML. This is called
	 * when a view is being constructed from an XML file, supplying attributes
	 * that were specified in the XML file. This version uses a default style of
	 * 0, so the only attribute values applied are those in the Context's Theme
	 * and the given AttributeSet.
	 * 
	 * The method onFinishInflate() will be called after all children have been
	 * added.
	 * 
	 * @param context
	 *            The Context the view is running in, through which it can
	 *            access the current theme, resources, etc.
	 * @param attrsThe
	 *            attributes of the XML tag that is inflating the view.
	 * @see ShapeButton#ShapeButton(Context, AttributeSet, int)
	 */
	public ShapeButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		initShapeButton();
		readAttributes(attrs);
	}

	/**
	 * Perform inflation from XML and apply a class-specific base style. This
	 * constructor of View allows subclasses to use their own base style when
	 * they are inflating. For example, a Button class's constructor would call
	 * this version of the super class constructor and supply R.attr.buttonStyle
	 * for defStyle; this allows the theme's button style to modify all of the
	 * base view attributes (in particular its background) as well as the Button
	 * class's attributes.
	 * 
	 * @param context
	 *            The Context the view is running in, through which it can
	 *            access the current theme, resources, etc.
	 * @param attrs
	 *            The attributes of the XML tag that is inflating the view.
	 * @param defStyle
	 *            The default style to apply to this view. If 0, no style will
	 *            be applied (beyond what is included in the theme). This may
	 *            either be an attribute resource, whose value will be retrieved
	 *            from the current theme, or an explicit style resource.
	 * @see #ShapeButton(Context, AttributeSet)
	 */

	public ShapeButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initShapeButton();
		readAttributes(attrs);
	}

	/**
	 * Register a callback to be invoked when this view is clicked. If this view
	 * is not clickable, it becomes clickable.
	 * 
	 * @param l
	 *            The callback that will run
	 * 
	 * @see #setClickable(boolean)
	 */
	public void setOnClickListener(OnClickListener l) {
		mListener = l;
	}

	/**
	 * @param shape
	 *            the shape to add to the button
	 */
	public void addShape(ShapeElement shape) {
		mShapes.add(shape);

		Collections.sort(mShapes);

		invalidate();
	}

	/**
	 * @see android.view.View#onMeasure(int, int)
	 */
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
		final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
		final int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
		final int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

		// Compute needed width
		int neededWidth = 0, neededHeight = 0;
		for (ShapeElement shape : mShapes) {
			neededWidth = Math.max(neededWidth, shape.getNeededWidth());
			neededHeight = Math.max(neededHeight, shape.getNeededHeight());
		}

		neededWidth += getPaddingLeft() + getPaddingRight();
		neededHeight += getPaddingTop() + getPaddingBottom();

		// Adapt width to constraints
		switch (widthSpecMode) {
		case MeasureSpec.EXACTLY:
			neededWidth = widthSpecSize;
			break;
		case MeasureSpec.AT_MOST:
			neededWidth = Math.min(widthSpecSize, neededWidth);
			break;
		case MeasureSpec.UNSPECIFIED:
		default:
			break;
		}

		// Adapt height to constraints
		switch (heightSpecMode) {
		case MeasureSpec.EXACTLY:
			neededHeight = heightSpecSize;
			break;
		case MeasureSpec.AT_MOST:
			neededHeight = Math.min(heightSpecSize, neededHeight);
			break;
		case MeasureSpec.UNSPECIFIED:
		default:
			break;
		}

		setMeasuredDimension(neededWidth, neededHeight);

	}

	/**
	 * @see android.view.View#onDraw(android.graphics.Canvas)
	 */
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (isInEditMode()) {
			return;
		}

		// apply padding
		canvas.translate(getPaddingLeft(), getPaddingTop());

		for (ShapeElement shape : mShapes) {
			shape.onDraw(canvas);
		}
	}

	/**
	 * @see android.view.View#onTouchEvent(android.view.MotionEvent)
	 */
	public boolean onTouchEvent(MotionEvent event) {

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			int count = mShapes.size();
			mSelectedShape = -1;
			for (int i = count - 1; i >= 0; --i) {
				if (mShapes.get(i).checkTouch(event.getX() + getPaddingLeft(),
						event.getY() + getPaddingRight())) {
					mSelectedShape = i;
					mShapes.get(i).setPressed(true);
					invalidate();
					break;
				}
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (mSelectedShape >= 0) {
				if (!mShapes.get(mSelectedShape).checkTouch(
						event.getX() + getPaddingLeft(),
						event.getY() + getPaddingRight())) {
					mShapes.get(mSelectedShape).setPressed(false);
					invalidate();
					mSelectedShape = -1;
				}
			}
			break;
		case MotionEvent.ACTION_UP:
			if (mSelectedShape >= 0) {
				if (mShapes.get(mSelectedShape).checkTouch(
						event.getX() + getPaddingLeft(),
						event.getY() + getPaddingRight())) {
					if (mListener != null) {
						mListener.onClick(this, mShapes.get(mSelectedShape)
								.getId());
					}
				}
				mShapes.get(mSelectedShape).setPressed(false);
				invalidate();
			}
			break;
		}

		return (mSelectedShape >= 0);
	}

	/**
	 * Set the Xml resource to read the shapes settings from
	 * 
	 * @param xmlRes
	 */
	public void setShapesXml(int xmlRes) {
		try {
			readShapesXml(xmlRes);
		} catch (Exception e) {
			throw new RuntimeException("Error while reading shapes xml", e);
		}
	}

	/**
	 * @param shapeId
	 * @param enabled
	 */
	public void setShapeEnabled(final String shapeId, final boolean enabled) {
		if (shapeId == null) {
			return;
		}

		for (ShapeElement elem : mShapes) {
			if (shapeId.equals(elem.getId())) {
				elem.setEnabled(enabled);
				invalidate(); // invalidate the rect only
			}
		}
	}

	/**	
	 * Initializes the specifics for a shape button
	 */
	private void initShapeButton() {
		setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		mShapes = new ArrayList<ShapeElement>();
		mDipToPixel = getContext().getResources().getDisplayMetrics().density;

		if (isInEditMode()) {
			return;
		}
	}

	/**
	 * Read the attributes taken from XML
	 * 
	 * @param attrs
	 *            The attributes of the XML tag that is inflating the view.
	 */
	private void readAttributes(final AttributeSet attrs) {

		if (isInEditMode()) {
			return;
		}

		TypedArray a = getContext().obtainStyledAttributes(attrs,
				R.styleable.ShapeButton);

		int shapes = a.getResourceId(R.styleable.ShapeButton_shapes, 0);
		if (shapes != 0) {
			try {
				readShapesXml(shapes);
			} catch (Exception e) {
				throw new RuntimeException("Error while reading shapes xml", e);
			}
		}

		a.recycle();
	}

	/**
	 * 
	 * @param xmlRes
	 * @throws XmlPullParserException
	 */
	private void readShapesXml(int xmlRes) throws XmlPullParserException,
			IOException {
		XmlPullParser xpp = getContext().getResources().getXml(xmlRes);

		String name;
		ShapeElement shape;
		int event = xpp.getEventType();
		while (event != XmlPullParser.END_DOCUMENT) {
			switch (event) {
			case XmlPullParser.START_TAG:
				name = xpp.getName();
				ShapeType type = getType(name);

				if (type != null) {
					shape = new ShapeElement(ShapeType.valueOf(name));
					readShapeConfiguration(xpp, shape);
					addShape(shape);
				}
				break;
			}
			event = xpp.next();
		}
	}

	/**
	 * @param name
	 *            the name of the shape
	 * @return the enum value or null
	 */
	private ShapeType getType(String name) {
		ShapeType res;

		try {
			res = ShapeType.valueOf(name);
		} catch (IllegalArgumentException e) {
			// not a valid shape name
			Log.w("SB", "Unknown shape type : " + name);
			res = null;
		}

		return res;

	}

	/**
	 * Reads the common values
	 * 
	 * @param xpp
	 *            the pull parser
	 * @param element
	 *            the element to read into
	 */
	private void readShapeConfiguration(final XmlPullParser xpp,
			final ShapeElement element) {
		int x, y, z, width, height, angle, start, end, thickness;
		x = y = z = width = height = angle = start = end = thickness = 0;
		int count = xpp.getAttributeCount();
		String name;
		for (int i = 0; i < count; ++i) {
			name = xpp.getAttributeName(i);
			if ("x".equalsIgnoreCase(name)) {
				x = getPixelSize(xpp.getAttributeValue(i));
			} else if ("y".equalsIgnoreCase(name)) {
				y = getPixelSize(xpp.getAttributeValue(i));
			} else if ("z".equalsIgnoreCase(name)) {
				z = getPixelSize(xpp.getAttributeValue(i));
			} else if ("width".equalsIgnoreCase(name)) {
				width = getPixelSize(xpp.getAttributeValue(i));
			} else if ("height".equalsIgnoreCase(name)) {
				height = getPixelSize(xpp.getAttributeValue(i));
			} else if ("thickness".equalsIgnoreCase(name)) {
				thickness = getPixelSize(xpp.getAttributeValue(i));
			} else if ("angle".equalsIgnoreCase(name)) {
				angle = Integer.valueOf(xpp.getAttributeValue(i));
			} else if ("start".equalsIgnoreCase(name)) {
				start = Integer.valueOf(xpp.getAttributeValue(i));
			} else if ("end".equalsIgnoreCase(name)) {
				end = Integer.valueOf(xpp.getAttributeValue(i));
			} else if ("id".equalsIgnoreCase(name)) {
				element.setId(xpp.getAttributeValue(i));
			} else if ("base".equalsIgnoreCase(name)) {
				element.setBaseColor(Color.parseColor(xpp.getAttributeValue(i)));
			} else if ("accent".equalsIgnoreCase(name)) {
				element.setAccentColor(Color.parseColor(xpp
						.getAttributeValue(i)));
			}
		}

		element.setPosition(x, y, z);
		element.setSize(width, height);
		element.setAngle(angle);
		element.setRange(start, end);
		element.setThickness(thickness);
	}

	/**
	 * @param value
	 *            a value (dp, px, ...)
	 * @return the corresponding pixel value
	 */
	private int getPixelSize(String value) {
		int pixel;
		if (TextUtils.isEmpty(value)) {
			pixel = 0;
		} else if (value.endsWith("px")) {
			pixel = Integer.valueOf(value.substring(0, value.length() - 2));
		} else if (value.endsWith("dp")) {
			int dp = Integer.valueOf(value.substring(0, value.length() - 2));
			pixel = (int) ((dp * mDipToPixel) + 0.5f);
		} else {
			pixel = Integer.valueOf(value);
		}
		return pixel;
	}

	private List<ShapeElement> mShapes;
	private float mDipToPixel;
	private int mSelectedShape;
	private OnClickListener mListener;
}
