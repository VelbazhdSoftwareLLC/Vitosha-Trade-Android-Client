package eu.veldsoft.vitdiscomp;

import java.util.Calendar;
import java.util.Random;

import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class VitoshaTradeWallpaperService extends WallpaperService {
	private static final Random PRNG = new Random();

	private static int top = 0;

	private static int left = 0;

	private static long delay = 0;

	private static int width = 0;

	private static int height = 0;

	private static boolean visible = false;

	private static Bitmap images[] = null;

	private static BasicNetwork network = new BasicNetwork();
	static {
		// TODO Load ANN structure from the remote server.

		int inputSize = 0;
		int hiddenSize = 0;
		int outputSize = 0;

		network.addLayer(new BasicLayer(null, true, inputSize));
		network.addLayer(new BasicLayer(new ActivationSigmoid(), true, hiddenSize));
		network.addLayer(new BasicLayer(new ActivationSigmoid(), false, outputSize));
		network.getStructure().finalizeStructure();
		network.reset();
	}

	private class WallpaperEngine extends Engine {
		private final Handler handler = new Handler();

		private final Runnable drawer = new Runnable() {
			@Override
			public void run() {
				draw();
			}
		};

		private void draw() {
			if (images == null) {
				return;
			}

			SurfaceHolder holder = getSurfaceHolder();
			Canvas canvas = null;

			Bitmap image = images[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % images.length];
			left = PRNG.nextInt(image.getWidth() - width);
			top = PRNG.nextInt(image.getHeight() - height);

			try {
				canvas = holder.lockCanvas();
				if (canvas != null) {
					canvas.drawBitmap(image, new Rect(left, top, left + width - 1, top + height - 1),
							new Rect(0, 0, width - 1, height - 1), null);
					// TODO Draw ANN info.
				}
			} finally {
				if (canvas != null) {
					holder.unlockCanvasAndPost(canvas);
				}
			}

			handler.removeCallbacks(drawer);
			if (visible == true) {
				handler.postDelayed(drawer, delay);
			}
		}

		public WallpaperEngine() {
			super();

			SharedPreferences preferences = PreferenceManager
					.getDefaultSharedPreferences(VitoshaTradeWallpaperService.this);

			top = Integer.parseInt(preferences.getString("top_corner", "0"));
			left = Integer.parseInt(preferences.getString("left_corner", "0"));
			delay = Long.parseLong(preferences.getString("loading", "" + 86400000));

			handler.post(drawer);
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			VitoshaTradeWallpaperService.this.visible = visible;

			if (visible == true) {
				handler.post(drawer);
			} else {
				handler.removeCallbacks(drawer);
			}
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			super.onSurfaceDestroyed(holder);
			VitoshaTradeWallpaperService.this.visible = false;
			handler.removeCallbacks(drawer);
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			VitoshaTradeWallpaperService.this.width = width;
			VitoshaTradeWallpaperService.this.height = height;
			super.onSurfaceChanged(holder, format, width, height);
		}
	}

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
