package eu.veldsoft.vitdiscomp;

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

import org.encog.engine.network.activation.ActivationFunction;
import org.encog.engine.network.activation.ActivationTANH;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.ml.data.market.MarketDataDescription;
import org.encog.ml.data.market.MarketDataType;
import org.encog.ml.data.market.MarketMLDataSet;
import org.encog.ml.data.market.TickerSymbol;
import org.encog.ml.data.market.loader.LoadedMarketData;
import org.encog.ml.data.market.loader.MarketLoader;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Background calculation unit.
 *
 * @author Todor Balabanov
 */
public class VitoshaTradeWallpaperService extends WallpaperService {

	/**
	 * Pseudo-random number generator.
	 */
	private static final Random PRNG = new Random();
	/**
	 * Space between visual spots in pixels.
	 */
	private static final int GAP_BETWEEN_PANELS = 10;
	/**
	 * Default training time delay.
	 */
	private static final long DEFAULT_DELAY = 86400000L;
	/**
	 * Identifiers for the backgourd resources images to be used as background.
	 */
	private static final int IMAGES_IDS[] = {
			  R.drawable.vitosha_mountain_dimitar_petarchev_001,
			  R.drawable.vitosha_mountain_dimitar_petarchev_002,
			  R.drawable.vitosha_mountain_dimitar_petarchev_003,
			  R.drawable.vitosha_mountain_dimitar_petarchev_004,
	};

	//TODO Images should be loaded from a remote image server.
	/**
	 * Panel backgroud color in order to be a part transperent from the real background.
	 */
	private static final int PANEL_BACKGROUND_COLOR =
			  Color.argb(63, 0, 0, 0);

	// TODO Put all colors in the settings dialog.
	/**
	 * Text color to be used in panels.
	 */
	private static final int PANEL_TEXT_COLOR =
			  Color.argb(95, 255, 255, 255);
	/**
	 * Colors used in the charts.
	 */
	private static final int CHART_COLORS[] = {
			  Color.argb(95, 0, 255, 0),
			  Color.argb(95, 255, 0, 0)};
	/**
	 * Colors used to visualize neural networks.
	 */
	private static final int ANN_COLORS[] = {
			  Color.argb(95, 0, 255, 0),
			  Color.argb(95, 255, 255, 255),
			  Color.argb(95, 0, 0, 255),
			  Color.argb(95, 255, 255, 255),
			  Color.argb(95, 255, 0, 0)};
	/**
	 * Time delay between neural network trainings.
	 */
	private static long delay = 0;
	/**
	 * Visible surface width.
	 */
	private static int screenWidth = 0;
	/**
	 * Visible surface height.
	 */
	private static int screenHeight = 0;
	/**
	 * Wallpaper visibility flag.
	 */
	private static boolean visible = false;
	/**
	 * List of information panels rectangles information.
	 */
	private static Rect panels[] = {new Rect(), new Rect(), new Rect()};
	/**
	 * Neural network object.
	 */
	private static BasicNetwork network = new BasicNetwork();
	/**
	 * Training examples data set.
	 */
	private static MLDataSet examples = null;
	/**
	 * Forecasted data.
	 */
	private static MLData forecast = null;
	/**
	 * Calculated output data.
	 */
	private static MLData output = null;
	/**
	 * Training rule object.
	 */
	private static ResilientPropagation train = null;

	/**
	 * Initialize static class members.
	 */
	static {
		// TODO Load ANN structure from the remote server.
		Map<NeuronType, Integer> counters = new HashMap<NeuronType, Integer>();
		counters.put(NeuronType.REGULAR, 0);
		counters.put(NeuronType.BIAS, 0);
		counters.put(NeuronType.INPUT, 0);
		counters.put(NeuronType.OUTPUT, 0);

		for (int type : InputData.NEURONS) {
			counters.put(NeuronType.valueOf(type),
					  counters.get(NeuronType.valueOf(type)) + 1);
		}

		int inputSize = counters.get(NeuronType.INPUT);
		int hiddenSize = counters.get(NeuronType.REGULAR);
		int outputSize = counters.get(NeuronType.OUTPUT);

		/*
		 * Network construction.
		 */
		network.addLayer(new BasicLayer(null,
				  true, inputSize));
		network.addLayer(new BasicLayer(new ActivationTANH(),
				  true, hiddenSize));
		network.addLayer(new BasicLayer(new ActivationTANH(),
				  false, outputSize));
		network.getStructure().finalizeStructure();
		network.reset();

		// TODO Load weights to the network.

		double values[] = InputData.RATES[PRNG.nextInt(InputData.RATES.length)];

		/*
		 * Data construction.
		 */
		MarketMLDataSet data = new MarketMLDataSet(new MarketLoader() {
			@Override
			public Collection<LoadedMarketData> load(TickerSymbol symbol,
																  Set<MarketDataType> types, Date start,
																  Date end) {
				Collection<LoadedMarketData> result = new
						  ArrayList<LoadedMarketData>();

				for (int i = 0; i < InputData.TIME.length; i++) {
					/*
					 * Data outside of the desired time frame are not loaded.
					 */
					if (InputData.TIME[i] < start.getTime() || end.getTime() < InputData.TIME[i]) {
						continue;
					}

					LoadedMarketData value = new LoadedMarketData(new
							  Date(InputData.TIME[i]), symbol);

					value.setData(MarketDataType.CLOSE, InputData.CLOSE[i]);
					value.setData(MarketDataType.HIGH, InputData.HIGH[i]);
					value.setData(MarketDataType.LOW, InputData.LOW[i]);
					value.setData(MarketDataType.OPEN, InputData.OPEN[i]);
					value.setData(MarketDataType.VOLUME, InputData.VOLUME[i]);
					value.setData(MarketDataType.ADJUSTED_CLOSE, InputData.CLOSE[i]);

					result.add(value);
				}

				return result;
			}
		}, inputSize, outputSize);

		MarketDataDescription description = new MarketDataDescription(new
				  TickerSymbol(InputData.SYMBOL),
				  (new MarketDataType[]{MarketDataType.CLOSE, MarketDataType.HIGH,
							 MarketDataType.LOW,
							 MarketDataType.OPEN})[(int) (Math.random() * 4)],
				  true, true);
		data.addDescription(description);
		data.load(new Date(InputData.TIME[0]), new
				  Date(InputData.TIME[InputData.TIME.length - 1]));
		data.generate();

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

		/*
		 * At the first index is the low value. At the second index is the high
		 * value.
		 *
		 * There is a problem with this approach, because some activation
		 * functions are zero if the argument is infinity.
		 *
		 * The fist layer has no activation function.
		 */
		double range[] = findLowAndHigh(network.getActivation(2));

		/*
		 * Prepare training set.
		 */
		double input[][] = new double[values.length -
				  (inputSize + outputSize)][inputSize];
		double target[][] = new double[values.length -
				  (inputSize + outputSize)][outputSize];
		for (int i = 0; i < values.length - (inputSize + outputSize); i++) {
			for (int j = 0; j < inputSize; j++) {
				input[i][j] = range[0] + (range[1] - range[0]) *
						  (values[i + j] - min) / (max - min);
			}
			for (int j = 0; j < outputSize; j++) {
				target[i][j] = range[0] + (range[1] - range[0]) *
						  (values[i + inputSize + j] - min) / (max - min);
			}
		}
		examples = new BasicMLDataSet(input, target);

		/*
		 * Prepare forecast set.
		 */
		input = new double[1][inputSize];
		for (int j = 0; j < inputSize; j++) {
			input[0][j] = range[0] + (range[1] - range[0]) *
					  (values[values.length - inputSize + j] - min) / (max - min);
		}
		forecast = new BasicMLData(input[0]);
	}

	/**
	 * Lowest and highest values of partucular activation function. It is used for time series scaling.
	 *
	 * @param activation Activation function object.
	 * @return Array with two values - loeset in the first index and highest in the second index.
	 */
	private static double[] findLowAndHigh(ActivationFunction activation) {
		/*
		 * Use range of double values.
		 */
		double check[] = {
				  Double.MIN_VALUE, -0.000001, -0.00001, -0.0001,
				  -0.001, -0.01, -0.1, -1, -10, -100, -1000,
				  -10000, -100000, -1000000, 0, 0.000001, 0.00001,
				  0.0001, 0.001, 0.01, 0.1, 1, 10, 100, 1000, 10000,
				  100000, 1000000, Double.MAX_VALUE};

		/*
		 * Map the range of double values to activation function output.
		 */
		activation.activationFunction(check, 0, check.length);

		/*
		 * Soft the result of the activation fuction output.
		 */
		Arrays.sort(check);

		/*
		 * Return minimum and maximum values of the activation function output.
		 */
		return new double[]{check[0], check[check.length - 1]};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Engine onCreateEngine() {
		return new WallpaperEngine();
	}

	/**
	 * Wallpaper engine class.
	 *
	 * @author Todor Balabanov
	 */
	private class WallpaperEngine extends Engine {

		/**
		 * Thread handler.
		 */
		private final Handler handler = new Handler();

		/**
		 * Paint object.
		 */
		private final Paint paint = new Paint();

		/**
		 * Neural network training cycle thread.
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
		 * Constructor without parameters.
		 */
		public WallpaperEngine() {
			super();
			handler.post(trainer);
		}

		/**
		 * Neural network prediction getter.
		 */
		private void predict() {
			if (forecast == null) {
				return;
			}

			output = network.compute(forecast);
		}

		/**
		 * Common drawing procedure.
		 */
		private void draw() {
			SurfaceHolder holder = getSurfaceHolder();
			Canvas canvas = null;

			try {
				canvas = holder.lockCanvas();

				if (canvas != null) {
					drawBackground(canvas);
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
		 * Single neural network training cycle.
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
		 * Background drawing procedure.
		 *
		 * @param canvas Canvas object for background.
		 */
		private void drawBackground(Canvas canvas) {
			// TODO Images should be loaded from an image server.
			/*
			 * Change picture according the day in the year.
			 */
			Bitmap image = BitmapFactory.decodeResource(
					  VitoshaTradeWallpaperService.this.getResources(),
					  IMAGES_IDS[Calendar.getInstance().
								 get(Calendar.DAY_OF_YEAR) % IMAGES_IDS.length]);

			/*
			 * Select random top-left corner for image clip.
			 */
			int left = PRNG.nextInt(image.getWidth() - screenWidth);
			int top = PRNG.nextInt(image.getHeight() - screenHeight);

			/*
			 * Clip part of the image.
			 */
			canvas.drawBitmap(image, new Rect(left, top,
								 left + screenWidth - 1,
								 top + screenHeight - 1),
					  new Rect(0, 0, screenWidth - 1,
								 screenHeight - 1), null);
		}

		/**
		 * Panels drawing procedure.
		 *
		 * @param canvas Canvas object for panels drawing.
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
		 * Currency pair info drawing procedure.
		 *
		 * @param canvas Canvas object for currency pair info drawing.
		 */
		private void drawCurrencyPairInfo(Canvas canvas) {
			/*
			 * Time series info.
			 */
			int textSize = (panels[0].bottom - panels[0].top) / 5;
			paint.setTextSize(textSize);
			paint.setColor(PANEL_TEXT_COLOR);
			canvas.drawText("" + InputData.SYMBOL,
					  GAP_BETWEEN_PANELS + panels[0].left,
					  GAP_BETWEEN_PANELS + panels[0].top + textSize, paint);
			canvas.drawText("" + TimePeriod.value(InputData.PERIOD),
					  GAP_BETWEEN_PANELS + panels[0].left,
					  GAP_BETWEEN_PANELS + panels[0].top + 2 * textSize, paint);
		}

		/**
		 * Forecast drawing procedure.
		 *
		 * @param canvas Canvas object for forecast drawing.
		 */
		private void drawForecast(Canvas canvas) {
			/*
			 * Forecast.
			 */
			int x = panels[1].left;
			int y = panels[1].bottom;
			int width = panels[1].right - panels[1].left;
			int height = panels[1].bottom - panels[1].top;

			/*
			 * Output layer activation function is used because input layer
			 * has no activation function.
			 */
			double range[] = findLowAndHigh(network.getActivation(2));

			/*
			 * Total number of values to be visualized.
			 */
			int numberOfValues = network.getLayerNeuronCount(0) +
					  network.getLayerNeuronCount(2);

			/*
			 * Visualize past data.
			 */
			paint.setColor(CHART_COLORS[0]);
			for (int i = 0; forecast.getData() != null &&
					  i < forecast.getData().length; i++) {
				int offset = (int) (height * (forecast.getData()[i] - range[0]) /
						  (range[1] - range[0]));
				for (int dx = 0; dx < width / numberOfValues; dx++) {
					canvas.drawLine(x, y, x, y - offset, paint);
					x++;
				}
			}

			/*
			 * Visualize future data.
			 */
			paint.setColor(CHART_COLORS[1]);
			for (int i = 0; output.getData() != null &&
					  i < output.getData().length; i++) {
				int offset = (int) (height * (output.getData()[i] - range[0]) /
						  (range[1] - range[0]));
				for (int dx = 0; dx < width / numberOfValues; dx++) {
					canvas.drawLine(x, y, x, y - offset, paint);
					x++;
				}
			}
		}

		/**
		 * Neural networ drawing procedure.
		 *
		 * @param canvas Canvas object for neural network drawing.
		 */
		private void drawAnn(Canvas canvas) {
			/*
			 * Artificial neural network.
			 */
			double topology[][] = {
					  forecast.getData(),
					  new double[network.getLayerNeuronCount(0) *
								 network.getLayerNeuronCount(1)],
					  new double[network.getLayerNeuronCount(1)],
					  new double[network.getLayerNeuronCount(1) *
								 network.getLayerNeuronCount(2)],
					  output.getData()
			};

			/*
			 * At the first index is the low value. At the second index is
			 * the high value.
			 *
			 * There is a problem with this approach, because some activation
			 * functions are zero if the argument is infinity.
			 *
			 * The fist layer has no activation function.
			 */
			double range[] = findLowAndHigh(network.getActivation(2));

			/*
			 * Scale input layer data.
			 */
			for (int i = 0; i < topology[0].length; i++) {
				topology[0][i] = (topology[0][i] - range[0]) /
						  (range[1] - range[0]);
			}

			/*
			 * Scale output layer data.
			 */
			for (int i = 0; i < topology[4].length; i++) {
				topology[4][i] = (topology[4][i] - range[0]) /
						  (range[1] - range[0]);
			}

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
			 * Hidden layer values. Activation function of the second layer
			 * is used for scaling.
			 */
			range = findLowAndHigh(network.getActivation(1));
			for (int i = 0; i < topology[2].length; i++) {
				topology[2][i] = (network.getLayerOutput(1, i) - range[0]) /
						  (range[1] - range[0]);
			}

			/*
			 * Weights normalization.
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
			int width = panels[2].right - panels[2].left;
			int height = panels[2].bottom - panels[2].top;
			for (int x = panels[2].left, k = 0; k < ANN_COLORS.length;
				  x += width / ANN_COLORS.length, k++) {
				for (int dx = 0; dx < width / ANN_COLORS.length; dx++) {
					for (int y = panels[2].top, l = 0; y < panels[2].bottom &&
							  l < topology[k].length; y += height / topology[k].length, l++) {
						for (int dy = 0; dy < height / topology[k].length; dy++) {
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
		 * {@inheritDoc}
		 */
		@Override
		public void onVisibilityChanged(boolean visible) {
			VitoshaTradeWallpaperService.visible = visible;

			/*
			 * Do calculations only if the wallpaper is visible.
			 */
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
		public void onSurfaceChanged(SurfaceHolder holder,
											  int format, int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);

			screenWidth = width;
			screenHeight = height;

			SharedPreferences preferences = PreferenceManager
					  .getDefaultSharedPreferences(
								 VitoshaTradeWallpaperService.this);

			int panelsSideSize = Integer.parseInt(
					  preferences.getString("sizing", "100"));

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
					panels[0].top = height / 2 - panelsSideSize / 2 -
							  GAP_BETWEEN_PANELS - panelsSideSize;
					panels[0].right = GAP_BETWEEN_PANELS + panelsSideSize;
					panels[0].bottom = height / 2 - panelsSideSize / 2 -
							  GAP_BETWEEN_PANELS;

					panels[1].left = GAP_BETWEEN_PANELS;
					panels[1].top = height / 2 - panelsSideSize / 2;
					panels[1].right = GAP_BETWEEN_PANELS + panelsSideSize;
					panels[1].bottom = height / 2 + panelsSideSize / 2;

					panels[2].left = GAP_BETWEEN_PANELS;
					panels[2].top = height / 2 + panelsSideSize / 2 +
							  GAP_BETWEEN_PANELS;
					panels[2].right = GAP_BETWEEN_PANELS + panelsSideSize;
					panels[2].bottom = height / 2 + panelsSideSize / 2 +
							  GAP_BETWEEN_PANELS + panelsSideSize;
					break;
				case "cc":
					panels[0].left = width / 2 - panelsSideSize / 2;
					panels[0].top = height / 2 - panelsSideSize / 2 -
							  GAP_BETWEEN_PANELS - panelsSideSize;
					panels[0].right = width / 2 + panelsSideSize / 2;
					panels[0].bottom = height / 2 - panelsSideSize / 2 -
							  GAP_BETWEEN_PANELS;

					panels[1].left = width / 2 - panelsSideSize / 2;
					panels[1].top = height / 2 - panelsSideSize / 2;
					panels[1].right = width / 2 + panelsSideSize / 2;
					panels[1].bottom = height / 2 + panelsSideSize / 2;

					panels[2].left = width / 2 - panelsSideSize / 2;
					panels[2].top = height / 2 + panelsSideSize / 2 +
							  GAP_BETWEEN_PANELS;
					panels[2].right = width / 2 + panelsSideSize / 2;
					panels[2].bottom = height / 2 + panelsSideSize / 2 +
							  GAP_BETWEEN_PANELS + panelsSideSize;
					break;
				case "rc":
					panels[0].left = width - panelsSideSize - GAP_BETWEEN_PANELS;
					panels[0].top = height / 2 - panelsSideSize / 2 -
							  GAP_BETWEEN_PANELS - panelsSideSize;
					panels[0].right = width - GAP_BETWEEN_PANELS;
					panels[0].bottom = height / 2 - panelsSideSize / 2 -
							  GAP_BETWEEN_PANELS;

					panels[1].left = width - panelsSideSize - GAP_BETWEEN_PANELS;
					panels[1].top = height / 2 - panelsSideSize / 2;
					panels[1].right = width - GAP_BETWEEN_PANELS;
					panels[1].bottom = height / 2 + panelsSideSize / 2;

					panels[2].left = width - panelsSideSize - GAP_BETWEEN_PANELS;
					panels[2].top = height / 2 + panelsSideSize / 2 +
							  GAP_BETWEEN_PANELS;
					panels[2].right = width - GAP_BETWEEN_PANELS;
					panels[2].bottom = height / 2 + panelsSideSize / 2 +
							  GAP_BETWEEN_PANELS + panelsSideSize;
					break;
				case "lb":
					panels[0].left = GAP_BETWEEN_PANELS;
					panels[0].top = height - 3 * GAP_BETWEEN_PANELS - 3 *
							  panelsSideSize;
					panels[0].right = GAP_BETWEEN_PANELS + panelsSideSize;
					panels[0].bottom = height - 3 * GAP_BETWEEN_PANELS - 2 *
							  panelsSideSize;

					panels[1].left = GAP_BETWEEN_PANELS;
					panels[1].top = height - 2 * GAP_BETWEEN_PANELS - 2 *
							  panelsSideSize;
					panels[1].right = GAP_BETWEEN_PANELS + panelsSideSize;
					panels[1].bottom = height - 2 * GAP_BETWEEN_PANELS -
							  panelsSideSize;

					panels[2].left = GAP_BETWEEN_PANELS;
					panels[2].top = height - GAP_BETWEEN_PANELS - panelsSideSize;
					panels[2].right = GAP_BETWEEN_PANELS + panelsSideSize;
					panels[2].bottom = height - GAP_BETWEEN_PANELS;
					break;
				case "cb":
					panels[0].left = width / 2 - panelsSideSize / 2;
					panels[0].top = height - 3 * GAP_BETWEEN_PANELS - 3 *
							  panelsSideSize;
					panels[0].right = width / 2 + panelsSideSize / 2;
					panels[0].bottom = height - 3 * GAP_BETWEEN_PANELS - 2 *
							  panelsSideSize;

					panels[1].left = width / 2 - panelsSideSize / 2;
					panels[1].top = height - 2 * GAP_BETWEEN_PANELS - 2 *
							  panelsSideSize;
					panels[1].right = width / 2 + panelsSideSize / 2;
					panels[1].bottom = height - 2 * GAP_BETWEEN_PANELS -
							  panelsSideSize;

					panels[2].left = width / 2 - panelsSideSize / 2;
					panels[2].top = height - GAP_BETWEEN_PANELS - panelsSideSize;
					panels[2].right = width / 2 + panelsSideSize / 2;
					panels[2].bottom = height - GAP_BETWEEN_PANELS;
					break;
				case "rb":
					panels[0].left = width - panelsSideSize - GAP_BETWEEN_PANELS;
					panels[0].top = height - 3 * GAP_BETWEEN_PANELS - 3 *
							  panelsSideSize;
					panels[0].right = width - GAP_BETWEEN_PANELS;
					panels[0].bottom = height - 3 * GAP_BETWEEN_PANELS - 2 *
							  panelsSideSize;

					panels[1].left = width - panelsSideSize - GAP_BETWEEN_PANELS;
					panels[1].top = height - 2 * GAP_BETWEEN_PANELS - 2 *
							  panelsSideSize;
					panels[1].right = width - GAP_BETWEEN_PANELS;
					panels[1].bottom = height - 2 * GAP_BETWEEN_PANELS -
							  panelsSideSize;

					panels[2].left = width - panelsSideSize - GAP_BETWEEN_PANELS;
					panels[2].top = height - GAP_BETWEEN_PANELS - panelsSideSize;
					panels[2].right = width - GAP_BETWEEN_PANELS;
					panels[2].bottom = height - GAP_BETWEEN_PANELS;
					break;
				default:
					break;
			}

			delay = Long.parseLong(preferences.getString("loading",
					  "" + DEFAULT_DELAY));
		}
	}
}
