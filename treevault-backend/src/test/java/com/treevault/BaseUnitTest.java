package com.treevault;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Base class for unit tests that provides common Mockito extension configuration.
 * 
 * <p>All unit tests should extend this class to inherit:
 * <ul>
 *   <li>MockitoExtension for automatic mock initialization</li>
 *   <li>Support for @Mock and @InjectMocks annotations</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>
 * class MyServiceTest extends BaseUnitTest {
 *     
 *     {@literal @}Mock
 *     private MyRepository repository;
 *     
 *     {@literal @}InjectMocks
 *     private MyService service;
 *     
 *     {@literal @}Test
 *     void shouldDoSomething() {
 *         // test code
 *     }
 * }
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseUnitTest {
}

