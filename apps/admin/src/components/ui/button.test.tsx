import React from 'react';
import { render, screen } from '@testing-library/react';
import { Button } from './button'; // Adjust path if necessary

describe('Button', () => {
  it('renders with default variant and size', () => {
    render(<Button>Test Button</Button>);
    const buttonElement = screen.getByText('Test Button');
    expect(buttonElement).toBeInTheDocument();
    expect(buttonElement).toHaveClass('bg-primary'); // Check for default variant class
  });

  it('renders with a different variant', () => {
    render(<Button variant="destructive">Delete</Button>);
    const buttonElement = screen.getByText('Delete');
    expect(buttonElement).toBeInTheDocument();
    expect(buttonElement).toHaveClass('bg-destructive'); // Check for destructive variant class
  });

  it('renders as a child when asChild is true', () => {
    render(<Button asChild><a href="/test">Link Button</a></Button>);
    const linkElement = screen.getByText('Link Button');
    expect(linkElement.tagName).toBe('A'); // Check if it renders as an anchor tag
    expect(linkElement).toHaveAttribute('href', '/test');
  });
});
