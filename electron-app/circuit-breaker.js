class APICircuitBreaker {
  constructor(threshold = 5, timeout = 60000, resetTimeout = 300000) {
    this.failureCount = 0;
    this.threshold = threshold;
    this.timeout = timeout;
    this.resetTimeout = resetTimeout;
    this.state = 'CLOSED'; // CLOSED, OPEN, HALF_OPEN
    this.nextAttempt = Date.now();
    this.lastFailureTime = null;
  }

  async call(fn, context = 'API') {
    if (this.state === 'OPEN') {
      if (Date.now() < this.nextAttempt) {
        const waitTime = Math.round((this.nextAttempt - Date.now()) / 1000);
        throw new Error(`Circuit breaker is OPEN for ${context}. Try again in ${waitTime}s`);
      }
      this.state = 'HALF_OPEN';
      console.log(`Circuit breaker transitioning to HALF_OPEN for ${context}`);
    }

    const startTime = Date.now();
    
    try {
      const result = await Promise.race([
        fn(),
        new Promise((_, reject) => 
          setTimeout(() => reject(new Error(`Timeout after ${this.timeout}ms`)), this.timeout)
        )
      ]);
      
      this.onSuccess(context, Date.now() - startTime);
      return result;
    } catch (error) {
      this.onFailure(context, error, Date.now() - startTime);
      throw error;
    }
  }

  onSuccess(context, duration) {
    this.failureCount = 0;
    if (this.state === 'HALF_OPEN') {
      this.state = 'CLOSED';
      console.log(`Circuit breaker CLOSED for ${context} (success after ${duration}ms)`);
    }
  }

  onFailure(context, error, duration) {
    this.failureCount++;
    this.lastFailureTime = Date.now();
    
    console.error(`Circuit breaker failure ${this.failureCount}/${this.threshold} for ${context}:`, error.message, `(${duration}ms)`);
    
    if (this.failureCount >= this.threshold) {
      this.state = 'OPEN';
      this.nextAttempt = Date.now() + this.resetTimeout;
      console.warn(`Circuit breaker OPENED for ${context}. Will retry after ${this.resetTimeout/1000}s`);
    }
  }

  getState() {
    return {
      state: this.state,
      failureCount: this.failureCount,
      nextAttempt: this.nextAttempt,
      lastFailureTime: this.lastFailureTime
    };
  }

  reset() {
    this.failureCount = 0;
    this.state = 'CLOSED';
    this.nextAttempt = Date.now();
    this.lastFailureTime = null;
    console.log('Circuit breaker manually reset');
  }
}

// Create global circuit breakers for different services
const circuitBreakers = {
  gcs: new APICircuitBreaker(5, 30000, 300000),    // GCS operations
  gemini: new APICircuitBreaker(3, 60000, 600000), // Gemini API calls
  posting: new APICircuitBreaker(3, 45000, 300000) // X posting operations
};

module.exports = { APICircuitBreaker, circuitBreakers };