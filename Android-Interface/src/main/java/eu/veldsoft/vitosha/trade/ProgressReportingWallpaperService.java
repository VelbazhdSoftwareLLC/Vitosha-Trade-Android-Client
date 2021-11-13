package eu.veldsoft.vitosha.trade;

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

import java.util.Calendar;
import java.util.Random;
import java.util.logging.Logger;

import eu.veldsoft.vitosha.trade.communication.HttpHelper;
import eu.veldsoft.vitosha.trade.communication.TimePeriod;
import eu.veldsoft.vitosha.trade.dummy.InputData;
import eu.veldsoft.vitosha.trade.engine.Predictor;

/**
 * Background calculation unit.
 *
 * @author Todor Balabanov
 */
public class ProgressReportingWallpaperService extends WallpaperService {
    /**
     * Logger instance.
     */
    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

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

    //TODO Images should be loaded from a remote image server.

    /**
     * Identifiers for the background resources images to be used as background.
     */
    private static final int[] IMAGES_IDS = {
            R.drawable.vitosha_mountain_dimitar_petarchev_001,
            R.drawable.vitosha_mountain_dimitar_petarchev_002,
            R.drawable.vitosha_mountain_dimitar_petarchev_003,
            R.drawable.vitosha_mountain_dimitar_petarchev_004,
    };

    // TODO Put all colors in the settings dialog.

    /**
     * Panel background color in order to be a part transparent from the real background.
     */
    private static final int PANEL_BACKGROUND_COLOR =
            Color.argb(63, 0, 0, 0);

    /**
     * Text color to be used in panels.
     */
    private static final int PANEL_TEXT_COLOR =
            Color.argb(95, 255, 255, 255);

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
    private static Rect[] panels = {new Rect(), new Rect(), new Rect()};

    /**
     * Forecasting object.
     */
    private static Predictor predictor = new Predictor();

    /**
     * Initialize common class members.
     */
    private void initialize() {
        /*
         * Load ANN structure and time series data from the remote server.
         */
        HttpHelper helper = new HttpHelper(PreferenceManager
                .getDefaultSharedPreferences(
                        ProgressReportingWallpaperService.this).
                        getString("server_url", "localhost"));

        if (helper.load() == false) {
            // TODO Use local data if the remote server is not available.
        }

        predictor.initialize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        super.onCreate();

        initialize();
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
                predictor.predict();
                draw();
                predictor.train();
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
                    ProgressReportingWallpaperService.this.getResources(),
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
            int width = panels[1].right - panels[1].left;
            int height = panels[1].bottom - panels[1].top;
            int stride = width;

            int[] pixels = new int[width * height];
            predictor.drawForecast(pixels, width, height);
            Bitmap bitmap = Bitmap.createBitmap(pixels, 0, stride, width, height, Bitmap.Config.ARGB_8888);
            canvas.drawBitmap(bitmap, new Rect(0, 0, width, height), panels[1], paint);
        }

        /**
         * Neural network drawing procedure.
         *
         * @param canvas Canvas object for neural network drawing.
         */
        private void drawAnn(Canvas canvas) {
            int width = panels[2].right - panels[2].left;
            int height = panels[2].bottom - panels[2].top;
            int stride = width;

            int[] pixels = new int[width * height];
            predictor.drawAnn(pixels, width, height);
            Bitmap bitmap = Bitmap.createBitmap(pixels, 0, stride, width, height, Bitmap.Config.ARGB_8888);
            canvas.drawBitmap(bitmap, new Rect(0, 0, width, height), panels[2], paint);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onVisibilityChanged(boolean visible) {
            ProgressReportingWallpaperService.visible = visible;

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
            ProgressReportingWallpaperService.visible = false;
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
                            ProgressReportingWallpaperService.this);

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
