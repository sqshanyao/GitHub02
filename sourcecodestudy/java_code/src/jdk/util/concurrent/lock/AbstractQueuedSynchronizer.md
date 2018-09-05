# 队列同步器

是用来构建锁或者其他同步组件的基础框架，它使用了一个int成员变量表示同步状态，**通过内置的FIFO队列来完成资源获取线程的排队工作**，并发包的作者（Doug Lea）期望它能够成为实现大部分同步需求的基础

同步器的主要使用方式是继承，**子类通过继承同步器并实现它的抽象方法来管理同步状
态**，在抽象方法的实现过程中免不了要对同步状态进行更改，这时就需要使用同步器提供的3
个方法（getState()、setState(int newState)和compareAndSetState(int expect,int update)）来进行操作，因为它们能够保证状态的改变是安全的。子类推荐被定义为自定义同步组件的静态内部类，同步器自身没有实现任何同步接口，它仅仅是定义了若干同步状态获取和释放的方法来供自定义同步组件使用，**同步器既可以支持独占式地获取同步状态，也可以支持共享式地获取同步状态**，这样就可以方便实现不同类型的同步组件（ReentrantLock、ReentrantReadWriteLock和CountDownLatch等）

同步器是实现锁（也可以是任意同步组件）的关键，在锁的实现中聚合同步器，利用同步
器实现锁的语义。可以这样理解二者之间的关系：**锁是面向使用者的**，它定义了使用者与锁交
互的接口（比如可以允许两个线程并行访问），隐藏了实现细节；**同步器面向的是锁的实现者**，
它简化了锁的实现方式，屏蔽了同步状态管理、线程的排队、等待与唤醒等底层操作。锁和同
步器很好地隔离了使用者和实现者所需关注的领域。

## 同步器中的接口

同步器的设计是基于**模板方法模式**的，也就是说，使用者需要继承同步器并重写指定的方法，随后将同步器组合在自定义同步组件的实现中


### 同步器提供的可重写接口

* tryAcquire
	独占式获取同步状态，实现方法需查询当前同步状态是否符合预期，然后在进行CAS设置同步状态，如下代码是ReentrantLocak实现的方法

		protected final boolean tryAcquire(int acquires) {
		            final Thread current = Thread.currentThread();
		            int c = getState();
		            if (c == 0) {
		                if (!hasQueuedPredecessors() &&
		                    compareAndSetState(0, acquires)) {
		                    setExclusiveOwnerThread(current);
		                    return true;
		                }
		            }
		            else if (current == getExclusiveOwnerThread()) {
		                int nextc = c + acquires;
		                if (nextc < 0)
		                    throw new Error("Maximum lock count exceeded");
		                setState(nextc);
		                return true;
		            }
		            return false;
		        }

* tryRelease
	独占式释放同步状态，如下代码是ReentrantLocak实现的方法

		        protected final boolean tryRelease(int releases) {
		            int c = getState() - releases;
		            if (Thread.currentThread() != getExclusiveOwnerThread())
		                throw new IllegalMonitorStateException();
		            boolean free = false;
		            if (c == 0) {
		                free = true;
		                setExclusiveOwnerThread(null);
		            }
		            setState(c);
		            return free;
		        }

* tryAcquireShared
	共享式获取同步状态，返回大于0标示获取成功，反之获取失败


* tryReleaseShared
	共享式释放同步状态

* isHeldExclusively
	当前同步器是否在独占模式下被线程占用，一般标示是否被当前线程所独占

### 同步器已经提供的模板方法




