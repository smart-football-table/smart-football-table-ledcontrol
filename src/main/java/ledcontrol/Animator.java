package ledcontrol;

public interface Animator {

	public interface AnimatorTask {
		void stop();
	}

	AnimatorTask start(Runnable callable);

}