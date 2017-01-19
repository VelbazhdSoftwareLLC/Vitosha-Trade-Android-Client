package eu.veldsoft.vitdiscomp;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

/**
 * 
 * @author Todor Balabanov
 */
public class VitoshaTradeWallpaperService extends WallpaperService {
	/**
	 * 
	 */
	private static final Random PRNG = new Random();

	/**
	 * 
	 */
	private static final int GAP_BETWEEN_PANELS = 10;

	/**
	 * 
	 */
	private static final int PANEL_BACKGROUND_COLOR = Color.argb(63, 0, 0, 0);

	/**
	 * 
	 */
	private static final int PANEL_TEXT_COLOR = Color.argb(95, 255, 255, 255);

	/**
	 * 
	 */
	private static final int CHART_COLORS[] = { Color.argb(95, 0, 255, 0), Color.argb(95, 255, 0, 0) };

	/**
	 * 
	 */
	private static final int ANN_COLORS[] = { Color.argb(95, 0, 255, 0), Color.argb(95, 255, 255, 255),
			Color.argb(95, 0, 0, 255), Color.argb(95, 255, 255, 255), Color.argb(95, 255, 0, 0) };

	/**
	 * 
	 */
	private static long delay = 0;

	/**
	 * 
	 */
	private static int panelsSideSize = 0;

	/**
	 * 
	 */
	private static int width = 0;

	/**
	 * 
	 */
	private static int height = 0;

	/**
	 * 
	 */
	private static boolean visible = false;

	/**
	 * 
	 */
	private static Bitmap images[] = null;

	/**
	 * 
	 */
	private static Rect panels[] = { new Rect(), new Rect(), new Rect() };

	/**
	 * 
	 */
	private static BasicNetwork network = new BasicNetwork();

	/**
	 * 
	 */
	private static MLDataSet examples = null;

	/**
	 * 
	 */
	private static MLData forecast = null;

	/**
	 * 
	 */
	private static MLData output = null;

	/**
	 * 
	 */
	private static ResilientPropagation train = null;

	/**
	 * Initialize static class members.
	 */
	static {
		// TODO Load ANN structure from the remote server.
		Map<Integer, Integer> counters = new HashMap<Integer, Integer>();
		counters.put(NeuronType.REGULAR, 0);
		counters.put(NeuronType.BIAS, 0);
		counters.put(NeuronType.INPUT, 0);
		counters.put(NeuronType.OUTPUT, 0);

		for (int type : InputData.NEURONS) {
			counters.put(type, counters.get(type) + 1);
		}

		int inputSize = counters.get(NeuronType.INPUT);
		int hiddenSize = counters.get(NeuronType.REGULAR);
		int outputSize = counters.get(NeuronType.OUTPUT);

		/*
		 * Network construction.
		 */
		network.addLayer(new BasicLayer(null, true, inputSize));
		network.addLayer(new BasicLayer(new ActivationFadingSin(inputSize), true, hiddenSize));
		network.addLayer(new BasicLayer(new ActivationFadingSin(hiddenSize), false, outputSize));
		network.getStructure().finalizeStructure();
		network.reset();

		// TODO Load weights to the network.

		double values[] = InputData.RATES[PRNG.nextInt(InputData.RATES.length)];

		/*
		 * Normalize data.
		 */
		double min = values[0];
		double max = values[0];
		for (double value : values) {
			if (value < min) {
				min = value;
			}
			if (value > max) {
				max = value;
			}
		}

		// TODO Get activation function minimum and maximum in some better way.
		final double MIN = -0.99;
		final double MAX = +0.99;

		/*
		 * Prepare training set.
		 */
		double input[][] = new double[values.length - (inputSize + outputSize)][inputSize];
		double target[][] = new double[values.length - (inputSize + outputSize)][outputSize];
		for (int i = 0; i < values.length - (inputSize + outputSize); i++) {
			for (int j = 0; j < inputSize; j++) {
				input[i][j] = MIN + (MAX - MIN) * (values[i + j] - min) / (max - min);
			}
			for (int j = 0; j < outputSize; j++) {
				target[i][j] = MIN + (MAX - MIN) * (values[i + inputSize + j] - min) / (max - min);
			}
		}
		examples = new BasicMLDataSet(input, target);

		/*
		 * Prepare forecast set.
		 */
		input = new double[1][inputSize];
		for (int j = 0; j < inputSize; j++) {
			input[0][j] = MIN + (MAX - MIN) * (values[values.length - inputSize + j] - min) / (max - min);
		}
		forecast = new BasicMLData(input[0]);
	}

	/**
	 * 
	 * @author Todor Balabanov
	 */
	private class WallpaperEngine extends Engine {
		/**
		 * 
		 */
		private final Handler handler = new Handler();

		/**
		 * 
		 */
		private final Paint paint = new Paint();

		/**
		 * 
		 */
		private final Runnable trainer = new Runnable() {
			@Override
			public void run() {
				predict();
				draw();
				train();
			}
		};

		/**
		 * 
		 */
		private void train() {
			if (network == null) {
				return;
			}
			if (examples == null) {
				return;
			}

			train = new ResilientPropagation(network, examples);
			train.iteration();
			train.finishTraining();
		}

		/**
		 * 
		 */
		private void predict() {
			if (forecast == null) {
				return;
			}

			output = network.compute(forecast);
		}

		/**
		 * 
		 * @param canvas
		 */
		private void drawBackgroud(Canvas canvas) {
			/*
			 * Change picture according the day in the year.
			 */
			Bitmap image = images[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % images.length];

			/*
			 * Select random top-left corner for image clip.
			 */
			int left = PRNG.nextInt(image.getWidth() - width);
			int top = PRNG.nextInt(image.getHeight() - height);

			/*
			 * Clip part of the image.
			 */
			canvas.drawBitmap(image, new Rect(left, top, left + width - 1, top + height - 1),
					new Rect(0, 0, width - 1, height - 1), null);
		}

		/**
		 * 
		 * @param canvas
		 */
		private void drawPanels(Canvas canvas) {
			/*
			 * Panels.
			 */
			paint.setColor(PANEL_BACKGROUND_COLOR);
			for (Rect rectangle : panels) {
				canvas.drawRect(rectangle, paint);
			}
		}

		/**
		 * 
		 * @param canvas
		 */
		private void drawCurrencyPairInfo(Canvas canvas) {
			/*
			 * Time series info.
			 */
			int textSize = panelsSideSize / 5;
			paint.setTextSize(textSize);
			paint.setColor(PANEL_TEXT_COLOR);
			canvas.drawText("" + InputData.SYMBOL, GAP_BETWEEN_PANELS + panels[0].left,
					GAP_BETWEEN_PANELS + panels[0].top + textSize, paint);
			canvas.drawText("" + TimePeriod.value(InputData.PERIOD), GAP_BETWEEN_PANELS + panels[0].left,
					GAP_BETWEEN_PANELS + panels[0].top + 2 * textSize, paint);
		}

		/**
		 * 
		 * @param canvas
		 */
		private void drawForecast(Canvas canvas) {
			/*
			 * Forecast.
			 */
			int x = panels[1].left;
			int y = panels[1].bottom;

			paint.setColor(CHART_COLORS[0]);
			for (int i = 0; forecast.getData() != null && i < forecast.getData().length; i++) {
				for (int dx = 0; dx < panelsSideSize
						/ (network.getLayerNeuronCount(0) + network.getLayerNeuronCount(2)); dx++) {
					canvas.drawLine(x, y, x, y - (int) (forecast.getData()[i] * panelsSideSize), paint);
					x++;
				}
			}

			paint.setColor(CHART_COLORS[1]);
			for (int i = 0; output.getData() != null && i < output.getData().length; i++) {
				for (int dx = 0; dx < panelsSideSize
						/ (network.getLayerNeuronCount(0) + network.getLayerNeuronCount(2)); dx++) {
					canvas.drawLine(x, y, x, y - (int) (output.getData()[i] * panelsSideSize), paint);
					x++;
				}
			}
		}

		/**
		 * 
		 * @param canvas
		 */
		private void drawAnn(Canvas canvas) {
			/*
			 * Artificial neural network.
			 */
			double topology[][] = { forecast.getData(),
					new double[network.getLayerNeuronCount(0) * network.getLayerNeuronCount(1)],
					new double[network.getLayerNeuronCount(1)],
					new double[network.getLayerNeuronCount(1) * network.getLayerNeuronCount(2)], output.getData() };

			for (int i = 0, m = 0, n = 0; i < topology[1].length; i++) {
				if (n >= network.getLayerNeuronCount(1)) {
					n = 0;
					m++;
				}
				if (m >= network.getLayerNeuronCount(0)) {
					m = 0;
				}
				topology[1][i] = network.getWeight(0, m, n);
				n++;
			}

			for (int i = 0, m = 0, n = 0; i < topology[3].length; i++) {
				if (n >= network.getLayerNeuronCount(2)) {
					n = 0;
					m++;
				}
				if (m >= network.getLayerNeuronCount(1)) {
					m = 0;
				}
				topology[3][i] = network.getWeight(1, m, n);
				n++;
			}

			/*
			 * Hidden layer values.
			 */
			for (int i = 0; i < network.getLayerNeuronCount(1); i++) {
				topology[2][i] = network.getLayerOutput(1, i);
			}

			/*
			 * Normalize weights.
			 */
			double min = Double.MAX_VALUE;
			double max = Double.MIN_VALUE;
			for (double value : topology[1]) {
				if (value < min) {
					min = value;
				}
				if (value > max) {
					max = value;
				}
			}
			for (double value : topology[3]) {
				if (value < min) {
					min = value;
				}
				if (value > max) {
					max = value;
				}
			}
			for (int i = 0; i < topology[1].length; i++) {
				topology[1][i] = (topology[1][i] - min) / (max - min);
			}
			for (int i = 0; i < topology[3].length; i++) {
				topology[3][i] = (topology[3][i] - min) / (max - min);
			}

			/*
			 * Draw topology.
			 */
			for (int x = panels[2].left, k = 0; k < ANN_COLORS.length; x += panelsSideSize / ANN_COLORS.length, k++) {
				for (int dx = 0; dx < panelsSideSize / ANN_COLORS.length; dx++) {
					for (int y = panels[2].top, l = 0; y < panels[2].bottom
							&& l < topology[k].length; y += panelsSideSize / topology[k].length, l++) {
						for (int dy = 0; dy < panelsSideSize / topology[k].length; dy++) {
							paint.setColor(ANN_COLORS[k]);
							paint.setColor(Color.argb(Color.alpha(ANN_COLORS[k]),
									(int) (Color.red(ANN_COLORS[k]) * topology[k][l]),
									(int) (Color.green(ANN_COLORS[k]) * topology[k][l]),
									(int) (Color.blue(ANN_COLORS[k]) * topology[k][l])));
							canvas.drawPoint(x + dx, y + dy, paint);
						}
					}
				}
			}
		}

		/**
		 * 
		 */
		private void draw() {
			if (images == null) {
				return;
			}

			SurfaceHolder holder = getSurfaceHolder();
			Canvas canvas = null;

			try {
				canvas = holder.lockCanvas();

				if (canvas != null) {
					drawBackgroud(canvas);

					drawPanels(canvas);

					drawCurrencyPairInfo(canvas);

					drawForecast(canvas);

					drawAnn(canvas);
				}
			} finally {
				if (canvas != null) {
					holder.unlockCanvasAndPost(canvas);
				}
			}

			handler.removeCallbacks(trainer);
			if (visible == true) {
				handler.postDelayed(trainer, delay);
			}
		}

		/**
		 * 
		 */
		public WallpaperEngine() {
			super();

			handler.post(trainer);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onVisibilityChanged(boolean visible) {
			VitoshaTradeWallpaperService.visible = visible;

			if (visible == true) {
				handler.post(trainer);
			} else {
				handler.removeCallbacks(trainer);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			super.onSurfaceDestroyed(holder);
			VitoshaTradeWallpaperService.visible = false;
			handler.removeCallbacks(trainer);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);

			VitoshaTradeWallpaperService.width = width;
			VitoshaTradeWallpaperService.height = height;

			SharedPreferences preferences = PreferenceManager
					.getDefaultSharedPreferences(VitoshaTradeWallpaperService.this);

			panelsSideSize = Integer.parseInt(preferences.getString("sizing", "100"));
			switch (preferences.getString("positioning", "0 0")) {
			case "lt":
				panels[0].left = GAP_BETWEEN_PANELS;
				panels[0].top = GAP_BETWEEN_PANELS;
				panels[0].right = GAP_BETWEEN_PANELS + panelsSideSize;
				panels[0].bottom = panelsSideSize + GAP_BETWEEN_PANELS;

				panels[1].left = GAP_BETWEEN_PANELS;
				panels[1].top = 2 * GAP_BETWEEN_PANELS + panelsSideSize;
				panels[1].right = GAP_BETWEEN_PANELS + panelsSideSize;
				panels[1].bottom = 2 * GAP_BETWEEN_PANELS + 2 * panelsSideSize;

				panels[2].left = GAP_BETWEEN_PANELS;
				panels[2].top = 3 * GAP_BETWEEN_PANELS + 2 * panelsSideSize;
				panels[2].right = GAP_BETWEEN_PANELS + panelsSideSize;
				panels[2].bottom = 3 * GAP_BETWEEN_PANELS + 3 * panelsSideSize;
				break;
			case "ct":
				panels[0].left = width / 2 - panelsSideSize / 2;
				panels[0].top = GAP_BETWEEN_PANELS;
				panels[0].right = width / 2 + panelsSideSize / 2;
				panels[0].bottom = panelsSideSize + GAP_BETWEEN_PANELS;

				panels[1].left = width / 2 - panelsSideSize / 2;
				panels[1].top = 2 * GAP_BETWEEN_PANELS + panelsSideSize;
				panels[1].right = width / 2 + panelsSideSize / 2;
				panels[1].bottom = 2 * GAP_BETWEEN_PANELS + 2 * panelsSideSize;

				panels[2].left = width / 2 - panelsSideSize / 2;
				panels[2].top = 3 * GAP_BETWEEN_PANELS + 2 * panelsSideSize;
				panels[2].right = width / 2 + panelsSideSize / 2;
				panels[2].bottom = 3 * GAP_BETWEEN_PANELS + 3 * panelsSideSize;
				break;
			case "rt":
				panels[0].left = width - panelsSideSize - GAP_BETWEEN_PANELS;
				panels[0].top = GAP_BETWEEN_PANELS;
				panels[0].right = width - GAP_BETWEEN_PANELS;
				panels[0].bottom = panelsSideSize + GAP_BETWEEN_PANELS;

				panels[1].left = width - panelsSideSize - GAP_BETWEEN_PANELS;
				panels[1].top = 2 * GAP_BETWEEN_PANELS + panelsSideSize;
				panels[1].right = width - GAP_BETWEEN_PANELS;
				panels[1].bottom = 2 * GAP_BETWEEN_PANELS + 2 * panelsSideSize;

				panels[2].left = width - panelsSideSize - GAP_BETWEEN_PANELS;
				panels[2].top = 3 * GAP_BETWEEN_PANELS + 2 * panelsSideSize;
				panels[2].right = width - GAP_BETWEEN_PANELS;
				panels[2].bottom = 3 * GAP_BETWEEN_PANELS + 3 * panelsSideSize;
				break;
			case "lc":
				panels[0].left = GAP_BETWEEN_PANELS;
				panels[0].top = height / 2 - panelsSideSize / 2 - GAP_BETWEEN_PANELS - panelsSideSize;
				panels[0].right = GAP_BETWEEN_PANELS + panelsSideSize;
				panels[0].bottom = height / 2 - panelsSideSize / 2 - GAP_BETWEEN_PANELS;

				panels[1].left = GAP_BETWEEN_PANELS;
				panels[1].top = height / 2 - panelsSideSize / 2;
				panels[1].right = GAP_BETWEEN_PANELS + panelsSideSize;
				panels[1].bottom = height / 2 + panelsSideSize / 2;

				panels[2].left = GAP_BETWEEN_PANELS;
				panels[2].top = height / 2 + panelsSideSize / 2 + GAP_BETWEEN_PANELS;
				panels[2].right = GAP_BETWEEN_PANELS + panelsSideSize;
				panels[2].bottom = height / 2 + panelsSideSize / 2 + GAP_BETWEEN_PANELS + panelsSideSize;
				break;
			case "cc":
				panels[0].left = width / 2 - panelsSideSize / 2;
				panels[0].top = height / 2 - panelsSideSize / 2 - GAP_BETWEEN_PANELS - panelsSideSize;
				panels[0].right = width / 2 + panelsSideSize / 2;
				panels[0].bottom = height / 2 - panelsSideSize / 2 - GAP_BETWEEN_PANELS;

				panels[1].left = width / 2 - panelsSideSize / 2;
				panels[1].top = height / 2 - panelsSideSize / 2;
				panels[1].right = width / 2 + panelsSideSize / 2;
				panels[1].bottom = height / 2 + panelsSideSize / 2;

				panels[2].left = width / 2 - panelsSideSize / 2;
				panels[2].top = height / 2 + panelsSideSize / 2 + GAP_BETWEEN_PANELS;
				panels[2].right = width / 2 + panelsSideSize / 2;
				panels[2].bottom = height / 2 + panelsSideSize / 2 + GAP_BETWEEN_PANELS + panelsSideSize;
				break;
			case "rc":
				panels[0].left = width - panelsSideSize - GAP_BETWEEN_PANELS;
				panels[0].top = height / 2 - panelsSideSize / 2 - GAP_BETWEEN_PANELS - panelsSideSize;
				panels[0].right = width - GAP_BETWEEN_PANELS;
				panels[0].bottom = height / 2 - panelsSideSize / 2 - GAP_BETWEEN_PANELS;

				panels[1].left = width - panelsSideSize - GAP_BETWEEN_PANELS;
				panels[1].top = height / 2 - panelsSideSize / 2;
				panels[1].right = width - GAP_BETWEEN_PANELS;
				panels[1].bottom = height / 2 + panelsSideSize / 2;

				panels[2].left = width - panelsSideSize - GAP_BETWEEN_PANELS;
				panels[2].top = height / 2 + panelsSideSize / 2 + GAP_BETWEEN_PANELS;
				panels[2].right = width - GAP_BETWEEN_PANELS;
				panels[2].bottom = height / 2 + panelsSideSize / 2 + GAP_BETWEEN_PANELS + panelsSideSize;
				break;
			case "lb":
				panels[0].left = GAP_BETWEEN_PANELS;
				panels[0].top = height - 3 * GAP_BETWEEN_PANELS - 3 * panelsSideSize;
				panels[0].right = GAP_BETWEEN_PANELS + panelsSideSize;
				panels[0].bottom = height - 3 * GAP_BETWEEN_PANELS - 2 * panelsSideSize;

				panels[1].left = GAP_BETWEEN_PANELS;
				panels[1].top = height - 2 * GAP_BETWEEN_PANELS - 2 * panelsSideSize;
				panels[1].right = GAP_BETWEEN_PANELS + panelsSideSize;
				panels[1].bottom = height - 2 * GAP_BETWEEN_PANELS - panelsSideSize;

				panels[2].left = GAP_BETWEEN_PANELS;
				panels[2].top = height - GAP_BETWEEN_PANELS - panelsSideSize;
				panels[2].right = GAP_BETWEEN_PANELS + panelsSideSize;
				panels[2].bottom = height - GAP_BETWEEN_PANELS;
				break;
			case "cb":
				panels[0].left = width / 2 - panelsSideSize / 2;
				panels[0].top = height - 3 * GAP_BETWEEN_PANELS - 3 * panelsSideSize;
				panels[0].right = width / 2 + panelsSideSize / 2;
				panels[0].bottom = height - 3 * GAP_BETWEEN_PANELS - 2 * panelsSideSize;

				panels[1].left = width / 2 - panelsSideSize / 2;
				panels[1].top = height - 2 * GAP_BETWEEN_PANELS - 2 * panelsSideSize;
				panels[1].right = width / 2 + panelsSideSize / 2;
				panels[1].bottom = height - 2 * GAP_BETWEEN_PANELS - panelsSideSize;

				panels[2].left = width / 2 - panelsSideSize / 2;
				panels[2].top = height - GAP_BETWEEN_PANELS - panelsSideSize;
				panels[2].right = width / 2 + panelsSideSize / 2;
				panels[2].bottom = height - GAP_BETWEEN_PANELS;
				break;
			case "rb":
				panels[0].left = width - panelsSideSize - GAP_BETWEEN_PANELS;
				panels[0].top = height - 3 * GAP_BETWEEN_PANELS - 3 * panelsSideSize;
				panels[0].right = width - GAP_BETWEEN_PANELS;
				panels[0].bottom = height - 3 * GAP_BETWEEN_PANELS - 2 * panelsSideSize;

				panels[1].left = width - panelsSideSize - GAP_BETWEEN_PANELS;
				panels[1].top = height - 2 * GAP_BETWEEN_PANELS - 2 * panelsSideSize;
				panels[1].right = width - GAP_BETWEEN_PANELS;
				panels[1].bottom = height - 2 * GAP_BETWEEN_PANELS - panelsSideSize;

				panels[2].left = width - panelsSideSize - GAP_BETWEEN_PANELS;
				panels[2].top = height - GAP_BETWEEN_PANELS - panelsSideSize;
				panels[2].right = width - GAP_BETWEEN_PANELS;
				panels[2].bottom = height - GAP_BETWEEN_PANELS;
				break;
			default:
				break;
			}
			delay = Long.parseLong(preferences.getString("loading", "" + 86400000));

		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Engine onCreateEngine() {
		// TODO Find better place to initialize images.
		if (images == null) {
			images = new Bitmap[] {
					BitmapFactory.decodeResource(this.getResources(),
							R.drawable.vitosha_mountain_dimitar_petarchev_001),
					BitmapFactory.decodeResource(this.getResources(),
							R.drawable.vitosha_mountain_dimitar_petarchev_002),
					BitmapFactory.decodeResource(this.getResources(),
							R.drawable.vitosha_mountain_dimitar_petarchev_003),
					BitmapFactory.decodeResource(this.getResources(),
							R.drawable.vitosha_mountain_dimitar_petarchev_004), };
		}

		return new WallpaperEngine();
	}
}
