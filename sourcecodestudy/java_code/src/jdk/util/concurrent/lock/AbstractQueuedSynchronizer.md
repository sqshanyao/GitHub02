# 队列同步器

是用来构建锁或者其他同步组件的基础框架，它使用了一个int成员变量表示同步状态，**通过内置的FIFO队列来完成资源获取线程的排队工作**

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

### 一些名词

* 独占式
	顾名思义同一时刻只能一个线程获取锁

* 共享式
	同一时刻可以多个线程获取锁，如在读写锁中读锁就是共享

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

* void acquire(int arg)
	独占式获取同步状态，如果获取成功，则由该方法返回，否则将进入同步队列等待，该方法将会调用重写的tryAcquire(int arg)方法，**该方法不响应中断**，代码详情下面有

* void acquireInterruptibly(int arg)
	该方法与acquire(int arg)一样，**但是该方法会响应中断**，如果当前线程被中断则该方法会抛出InterruptedException，代码详情下面有
	
* acquireShared(int arg)
	与acquire(int arg)一样但是该方法在**同一时刻可以有多个线程获取同步状态**，代码详情下面有
* acquireSharedInterruptibly
	与acquireInterruptibly(int arg)一样，但**该方法会响应中断**，代码详情下面有

* boolean release(int arg)
	独占式释放同步状态，释放后，将同步队列中的第一个节点包含的线程唤醒，代码详情下面有
* boolean releaseShared(int arg)
	共享式释放同步状态，代码详情下面有
* Collection<Thread> getQueuedThreads()
	获取等待在同步队列上的线程集合，代码详情下面有

### 同步器中的节点

用来保存获取同步状态失败的线程引用、等待状态以及前驱和后继节点
主要节点的属性类型与名称以及描述

* Node
	* int waitStatus;
	等待状态，包含以下状态
	 * CANCELLED，值为1，表示当前的线程被取消
	 * SIGNAL，值为-1，表示当前节点的后继节点包含的线程需要运行，当前节点的成如果释放或者取消通知后继节点
	 * CONDITION，值为-2，表示当前节点在等待condition，也就是在condition队列中
	 * PROPAGATE，值为-3，表示当前场景下后续的acquireShared能够得以执行
	 * 值为0，初始状态，等待着获取锁
	
	* Node prev;
	前驱节点
	* Node next;
	后继结点

	* Node nextWaiter;
	存储condition队列中的后继节点

	* Thread thread;
	入队列时的当前线程


* 同步队列

	没有获取到同步状态的节点会被加入到同步队列中，同步队列遵循FIFO，首节点是获取同步状态成功的节点，首节点的线程在释放同步状态时，将会唤醒后继节点，而后继节点将会在获取同步状态成功时将自己设置为首节点

	同步器提供了几个基于CASE的方法来设置同步队列的信息
	* boolean compareAndSetHead(Node update)
	* boolean compareAndSetTail(Node expect, Node update)
	* boolean compareAndSetNext(Node node, Node expect, Node update)
	* boolean compareAndSetWaitStatus(Node node, int expect, int update)


### 独占式同步状态获取与释放

* acquire


    public final void acquire(int arg) {
        if (!tryAcquire(arg) &&
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }

tryAcquire保证安全获取同步状态，这个**方法需要实现类自己去实现，如果获取失败构造同步节点**，独占式**Node.EXCLUSIVE**，同一时刻只能有一个线程成功获取同步状态）并通过**addWaiter(Node node)**方法将该节点加入到同步队列的尾部，**最后**调用**acquireQueued(Node node,int arg)**方法，使得该节点以“死循环”的方式获取同步状态。如果获取不到则阻塞节点中的线程，而被阻塞线程的唤醒主要依靠前驱节点的出队或阻塞线程被中断来实现。

* Node addWaiter(Node mode)
	将节点加入同步队列尾部
	    private Node addWaiter(Node mode) {
	        Node node = new Node(Thread.currentThread(), mode);
	        // Try the fast path of enq; backup to full enq on failure
	        Node pred = tail;
	        if (pred != null) {
	            node.prev = pred;
	            if (compareAndSetTail(pred, node)) {
	                pred.next = node;
	                return node;
	            }
	        }
	        enq(node);
	        return node;
	    }

* Node enq(final Node node)
 通过自旋（死循环）不停设置尾节点，因为同一时刻可能有多个线程插入
	    private Node enq(final Node node) {
	        for (;;) {
	            Node t = tail;
	            if (t == null) { // Must initialize
	                if (compareAndSetHead(new Node()))
	                    tail = head;
	            } else {
	                node.prev = t;
	                if (compareAndSetTail(t, node)) {
	                    t.next = node;
	                    return t;
	                }
	            }
	        }
	    }

* acquireQueued(Node node,int arg)

当节点加入到同步队列中后，在这里通过“死循环”尝试获取同步状态

	    final boolean acquireQueued(final Node node, int arg) {
	        boolean failed = true;
	        try {
	            boolean interrupted = false;
	            for (;;) {
	                final Node p = node.predecessor();
					//当前驱节点是头节点才能够尝试获取同步状态
	                if (p == head && tryAcquire(arg)) {
	                    setHead(node);
					//这里help GC是垃圾回收的一个小细节，设置前驱节点的后继节点为null
					//帮助垃圾回收器尽早将其回收
	                    p.next = null; // help GC
	                    failed = false;
	                    return interrupted;
	                }
	                if (shouldParkAfterFailedAcquire(p, node) &&
	                    parkAndCheckInterrupt())
	                    interrupted = true;
	            }
	        } finally {
	            if (failed)
	                cancelAcquire(node);
	        }
	    }

### 独占式同步状态释放
释放同步状态，该方法在释放了同步状态之后，会唤醒其后继节点（进而使后继节点重新尝试获取同步状态）

    public final boolean release(int arg) {
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0)
                unparkSuccessor(h);
            return true;
        }
        return false;
    }

### 共享式式同步状态获取与释放
* void acquireShared(int arg)
tryAcquireShared尝试获取同步状态，返回值大于等于0获取成功，返回值小于0进入doAcquireShared开始自旋获取同步状态


    public final void acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0)
            doAcquireShared(arg);
    }

* void doAcquireShared(int arg)
自旋共享式获取同步状态

	   private void doAcquireShared(int arg) {
			//将节点加入共享式同步队列中
	        final Node node = addWaiter(Node.SHARED);
	        boolean failed = true;
	        try {
	            boolean interrupted = false;
				//自旋获取同步状态
	            for (;;) {
	                final Node p = node.predecessor();
	                if (p == head) {
	                    int r = tryAcquireShared(arg);
	                    if (r >= 0) {
	                        setHeadAndPropagate(node, r);
	                        p.next = null; // help GC
	                        if (interrupted)
	                            selfInterrupt();
	                        failed = false;
	                        return;
	                    }
	                }
	                if (shouldParkAfterFailedAcquire(p, node) &&
	                    parkAndCheckInterrupt())
	                    interrupted = true;
	            }
	        } finally {
	            if (failed)
	                cancelAcquire(node);
	        }
	    }


* boolean releaseShared(int arg) 共享式释放同步状态

	    public final boolean releaseShared(int arg) {
	        if (tryReleaseShared(arg)) {
	            doReleaseShared();
	            return true;
	        }
	        return false;
	    }

### 独占式超时获取同步状态
成功获取同步器与前面的基本一样，但是获取失败后的处理有所改动，失败后判断nanosTimeout小于0则表示获取超时

    private boolean doAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);

                    p.next = null; // help GC
                    failed = false;
                    return true;
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    return false;
                if (shouldParkAfterFailedAcquire(p, node) &&
                    nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }
